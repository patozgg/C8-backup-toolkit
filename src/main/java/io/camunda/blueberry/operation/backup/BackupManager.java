package io.camunda.blueberry.operation.backup;


import io.camunda.blueberry.connect.*;
import io.camunda.blueberry.exception.BackupException;
import io.camunda.blueberry.exception.OperationException;
import io.camunda.blueberry.operation.OperationLog;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * This class are in charge to start a backup,
 */
@Component
public class BackupManager {
    private final ElasticSearchConnect elasticSearchConnect;
    Logger logger = LoggerFactory.getLogger(BackupManager.class);
    final OperateConnect operateConnect;
    final TaskListConnect taskListConnect;
    final OptimizeConnect optimizeConnect;
    final ZeebeConnect zeebeConnect;
    private BackupJob backupJob;

    public BackupManager(OperateConnect operateConnect, TaskListConnect taskListConnect, OptimizeConnect optimizeConnect, ZeebeConnect zeebeConnect, ElasticSearchConnect elasticSearchConnect) {
        this.operateConnect = operateConnect;
        this.taskListConnect = taskListConnect;
        this.optimizeConnect = optimizeConnect;
        this.zeebeConnect = zeebeConnect;
        this.elasticSearchConnect = elasticSearchConnect;
    }

    public synchronized void startBackup(BackupParameter backupParameter) throws OperationException {
        // Verify first is there is not already a backup in progress
        if (backupJob != null && backupJob.getJobStatus() == BackupJob.JOBSTATUS.INPROGRESS)
            throw new BackupException(null, 400, "Job Already in progress", "In Progress[" + backupJob.getBackupId() + "]", backupJob.getBackupId());
        // start a backup, asynchrously
        backupJob = new BackupJob(operateConnect, taskListConnect, optimizeConnect, zeebeConnect, elasticSearchConnect, new OperationLog());
        Long backupId = backupParameter.backupId;
        if (backupParameter.nextId) {
            logger.info("No backup is provided, calculate the new Id");
            // calculate a new backup ID
            long maxId = 0;
            try {
                List<BackupInfo> listBackup = getListBackup();
                for (BackupInfo info : listBackup) {
                    if (info.backupId > maxId)
                        maxId = info.backupId;
                }

            } catch (OperationException e) {
                logger.error("Error when accessing the list of Backup: {}", e);
                throw new BackupException(null, e.getStatus(), e.getError(), e.getMessage(), backupId);
            }
            backupId = maxId + 1;
            logger.info("No backupId is provided, calculate from the list +1 : {}", backupId);
        }
        // Start in a new thread
        startBackupAsynchronously(backupId);

    }

    private void startBackupAsynchronously(final long backupId) throws BackupException {
        new Thread(() -> {
            try {
                backupJob.backup(backupId);
            } catch (BackupException e) {
               logger.error("BackupId["+backupId+"] failed", e);
            }
        }).start();    }
    /**
     * Return the list of all backups visible on the platform
     *
     * @return
     */
    public List<BackupInfo> getListBackup() throws OperationException {
        return zeebeConnect.getListBackup();
    }

    /**
     * If a job is started, then a backupJob exist.
     * If the backup is terminated, then the backypJob is still available, with a status "TERMINATED"
     *
     * @return
     */
    public BackupJob getBackupJob() {
        return backupJob;
    }

    public static class BackupParameter {
        public boolean nextId;
        public Long backupId;
    }


}
