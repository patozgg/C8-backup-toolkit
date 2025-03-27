package io.camunda.blueberry.operation.backup;

import io.camunda.blueberry.connect.*;
import io.camunda.blueberry.exception.BackupException;
import io.camunda.blueberry.operation.OperationLog;

import java.util.List;

/**
 * Collect all operation of the backup. This is returned to the monitoring, or at the end of the execution
 */
public class BackupJob {

    private final OperateConnect operateConnect;
    private final TaskListConnect taskListConnect;
    private final OptimizeConnect optimizeConnect;
    private final ZeebeConnect zeebeConnect;
    private final ElasticSearchConnect elasticSearchConnect;

    private OperationLog operationLog;
    private JOBSTATUS jobStatus = JOBSTATUS.PLANNED;
    private long backupId;

    protected BackupJob(OperateConnect operateConnect, TaskListConnect taskListConnect, OptimizeConnect optimizeConnect,
                        ZeebeConnect zeebeConnect, ElasticSearchConnect elasticSearchConnect, OperationLog operationLog) {
        this.operateConnect = operateConnect;
        this.taskListConnect = taskListConnect;
        this.optimizeConnect = optimizeConnect;
        this.zeebeConnect = zeebeConnect;
        this.elasticSearchConnect = elasticSearchConnect;
        this.operationLog = operationLog;
    }

    public JOBSTATUS getJobStatus() {
        return jobStatus;
    }

    public OperationLog getOperationLog() {
        return operationLog;
    }

    public long getBackupId() {
        return backupId;
    }

    /**
     * Start a backup
     */
    public void backup(long backupId) throws BackupException {
        operationLog.startOperation("Backup", 7);

        this.jobStatus = JOBSTATUS.INPROGRESS;
        this.backupId = backupId; // calculate a new backup


        // Keep only applications existing in the cluster
        List<CamundaApplication> listApplications = List.of(operateConnect, taskListConnect, optimizeConnect)
                .stream()
                .filter(CamundaApplication::exist)
                .toList();



        // For each application, start the backup
        for (CamundaApplication application : listApplications) {
            operationLog.operationStep("backup "+application.getComponent().name());
            CamundaApplication.BackupOperation backupOperation = application.backup(backupId, operationLog);
            if (!backupOperation.isOk()) {
                this.jobStatus = JOBSTATUS.FAILED;
                throw new BackupException(application.getComponent(),
                        400,
                        backupOperation.title,
                        backupOperation.message, backupId);

            }
        }

        // Wait end of backup Operate, TaskList, Optimize, Zeebe
        for (CamundaApplication application : listApplications) {
            application.waitBackup(backupId, operationLog);
        }

        // Stop Zeebe imported : force to step 4
        operationLog.operationStep(4, "Pause Zeebe");
        zeebeConnect.pauseExporting(operationLog);

        // backup Zeebe record
        operationLog.operationStep(5, "Backup Zeebe Elasticsearch");
        elasticSearchConnect.esBackup(backupId, operationLog);

        // backup Zeebe
        operationLog.operationStep(6, "Backup Zeebe");
        zeebeConnect.backup(backupId, operationLog);
        zeebeConnect.monitorBackup(backupId, operationLog);

        // Finish? Then stop all restoration pod
        operationLog.operationStep(7, "Resume Zeebe");
        zeebeConnect.resumeExporting(operationLog);

        operationLog.endOperation();

        jobStatus = JOBSTATUS.COMPLETED;
    }

    public enum JOBSTATUS {PLANNED, INPROGRESS, COMPLETED, FAILED}
}
