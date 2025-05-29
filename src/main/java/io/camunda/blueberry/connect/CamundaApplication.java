package io.camunda.blueberry.connect;

import io.camunda.blueberry.exception.BackupException;
import io.camunda.blueberry.operation.OperationLog;

import java.util.ArrayList;
import java.util.List;

/**
 * Multiples component (Operate, TaskList, Optimize) react as the same way. To Simplify the management, they are mark as a "component"
 */
public interface CamundaApplication {
    boolean exist();

    BackupOperation backup(Long backupId, OperationLog operationLog) throws BackupException;

    void waitBackup(Long backupId, OperationLog operationLog) throws BackupException;

    /**
     * Return the component behind this application
     *
     * @return
     */
    COMPONENT getComponent();

    enum COMPONENT {TASKLIST, OPERATE, OPTIMIZE, ZEEBERECORD}

    class BackupOperation {
        public List<String> listSnapshots = new ArrayList<>();
        public int status;
        public String title;
        public String message;

        public boolean isOk() {
            return status == 200 || status == 202; // Accept both 200 (Operate, Tasklist) and 202 (Optimize)
        }
    }

    public static class ConnectionInfo {
        public boolean isConnected;
        public String explanations;
        public ConnectionInfo(boolean connected, String explanations) {
            this.isConnected = connected;
            this.explanations = explanations;
        }
    }
}
