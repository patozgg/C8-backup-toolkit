package io.camunda.blueberry.api;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.camunda.blueberry.connect.BackupInfo;
import io.camunda.blueberry.connect.ZeebeConnect;
import io.camunda.blueberry.config.ExplorationCluster;
import io.camunda.blueberry.operation.OperationLog;
import io.camunda.blueberry.operation.backup.BackupJob;
import io.camunda.blueberry.operation.backup.BackupManager;
import io.camunda.blueberry.platform.rule.Rule;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.ArrayList;
import java.util.List;

@RestController
@RequestMapping("blueberry")

public class DashboardRestController {
    private final ExplorationCluster explorationCluster;
    private final BackupManager backupManager;
    Logger logger = LoggerFactory.getLogger(DashboardRestController.class);

    public DashboardRestController(ExplorationCluster explorationCluster, BackupManager backupManager) {
        this.explorationCluster = explorationCluster;
        this.backupManager = backupManager;
    }

    @GetMapping(value = "/api/dashboard/all", produces = "application/json")
    public DashboardStatus all(@RequestParam(name = "forceRefresh", required = false, defaultValue = "false") boolean forceRefresh) {
        try {logger.debug("Rest [/api/dashboard/all]");
            DashboardStatus status = new DashboardStatus();

            if (forceRefresh) {
                // Force the refresh
                explorationCluster.refresh();
            }
            ZeebeConnect.ClusterInformation clusterInformation = explorationCluster.getClusterInformation();

            if (clusterInformation != null) {
                status.cluster.partitionsCount = clusterInformation.partitionsCount;
                status.cluster.replicationfactor = clusterInformation.replicationFactor;
                status.cluster.clusterSize = clusterInformation.clusterSize;

            }
            Boolean exporterStatus = explorationCluster.getExporterStatus();
            status.cluster.statusCluster = Boolean.TRUE.equals(exporterStatus) ? "ACTIF" : Boolean.FALSE.equals(exporterStatus) ? "DISABLED" : "";

            List<io.camunda.blueberry.connect.BackupInfo> listBackups = explorationCluster.getListBackup();
            if (listBackups!=null ) {
                status.backup.backupsCount = listBackups.size();
                status.backup.backupsFailed = (int) listBackups.stream().filter(t->t.status.equals(BackupInfo.Status.FAILED)).count();
                status.backup.backupsComplete = (int) listBackups.stream().filter(t->t.status.equals(BackupInfo.Status.COMPLETED)).count();
            }

            BackupJob backupJob = backupManager.getBackupJob();
            status.backup.statusBackup = "READY";
            if (backupJob != null) {
                status.backup.statusBackup=    backupJob.getJobStatus().toString();
                OperationLog operationLog = backupJob.getOperationLog();
                if (operationLog != null) {}
                    status.backup.step = operationLog.getCurrentStep()+"/"+operationLog.getTotalNumberOfSteps()+" ("+operationLog.getOperationName()+")";
            }


            status.scheduler.statusScheduler = "INACTIF";
            status.scheduler.cron = "";
            status.scheduler.next = "";
            status.scheduler.delay = "";

            Rule.RuleStatus ruleStatus = explorationCluster.rulesOk();
            status.configuration.statusConfiguration = ruleStatus == null ? "" : ruleStatus.toString();

            return status;

        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private class DashboardStatus {
        @JsonProperty
        DashClusterInfo cluster = new DashClusterInfo();
        @JsonProperty
        DashBackupInfo backup = new DashBackupInfo();
        @JsonProperty
        DashSchedulerInfo scheduler = new DashSchedulerInfo();
        @JsonProperty
        DashConfigurationInfo configuration = new DashConfigurationInfo();
    }

    private class DashClusterInfo {
        @JsonProperty
        Integer partitionsCount;
        @JsonProperty
        Integer replicationfactor;
        @JsonProperty
        Integer clusterSize;
        @JsonProperty
        String statusCluster; // "ACTIVE"
    }

    private class DashBackupInfo {
        @JsonProperty
        List<String> history = new ArrayList<>();
        @JsonProperty
        String statusBackup;
        @JsonProperty
        String step;
        @JsonProperty
        Integer backupsCount;
        @JsonProperty
        Integer backupsFailed;
        @JsonProperty
        Integer backupsComplete;
    }

    private class DashSchedulerInfo {
        @JsonProperty
        String statusScheduler; // "INACTIF",
        @JsonProperty
        String cron;
        @JsonProperty
        String next;
        @JsonProperty
        String delay;
    }

    private class DashConfigurationInfo {

        @JsonProperty
        String statusConfiguration;

    }
}