package io.camunda.blueberry.connect;

import io.camunda.blueberry.exception.BackupException;
import io.camunda.blueberry.operation.OperationLog;

import java.util.ArrayList;
import java.util.List;

/**
 * Multiples component (Operate, TaskList, Optimize) react as the same way. To Simplify the management, they are mark as a "component"
 */
public interface CamundaApplicationInt extends BackupComponentInt{
    boolean exist();

    /**
     * Return the component behind this application
     *
     * @return
     */
    COMPONENT getComponent();

    enum COMPONENT {TASKLIST, OPERATE, OPTIMIZE, ZEEBERECORD, ZEEBE}


    public static class ConnectionInfo {
        public boolean isConnected;
        public String explanations;
        public ConnectionInfo(boolean connected, String explanations) {
            this.isConnected = connected;
            this.explanations = explanations;
        }
    }
}
