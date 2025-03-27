package io.camunda.blueberry.operation.restoration;

import io.camunda.blueberry.operation.OperationLog;

public class RestorationJob {

    OperationLog operationLog;

    // Restore this backupId
    public RestorationJob(Long backupId) {
        OperationLog operationLog = new OperationLog();
    }

    /**
     * run asynchronously
     */
    public void restoration() {
        long beginTime = System.currentTimeMillis();
        operationLog.info("Start Restoration");

        // Scale down Zeebe

        // create one restore pod per cluster size, and start it

        // monitor each pod

        // Finish? Then stop all restoration pod

        // scale up Zeebe

        // restore elastic search

        operationLog.info("End of Restoration in " + (System.currentTimeMillis() - beginTime) + " ms");
    }
}
