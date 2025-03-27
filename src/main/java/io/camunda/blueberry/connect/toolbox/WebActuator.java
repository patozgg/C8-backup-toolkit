package io.camunda.blueberry.connect.toolbox;

import com.fasterxml.jackson.databind.JsonNode;
import io.camunda.blueberry.connect.ActuatorBackupStatusResponse;
import io.camunda.blueberry.connect.CamundaApplication;
import io.camunda.blueberry.exception.OperationException;
import io.camunda.blueberry.operation.OperationLog;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.RestTemplate;

import java.util.Map;

public class WebActuator {
    Logger logger = LoggerFactory.getLogger(WebActuator.class);
    @Autowired
    private RestTemplate restTemplate;

    public WebActuator(RestTemplate restTemplate) {
        this.restTemplate = restTemplate;
    }


    /**
     * Start the backup command
     * Example curl -X POST http://localhost:8081/actuator/backup -H "Content-Type: application/json" -d '{"backupId": 12}'
     *
     * @param component    component to communicate with
     * @param backupId     Backup ID
     * @param url          complete url to start the backup command in the component
     * @param operationLog log the operation
     */
    public CamundaApplication.BackupOperation startBackup(CamundaApplication.COMPONENT component, Long backupId, String url, OperationLog operationLog) {
        Map<String, Object> backupBody = java.util.Map.of("backupId", backupId);

        String urlComplete = url + "/actuator/backups";
        CamundaApplication.BackupOperation backupOperation = new CamundaApplication.BackupOperation();
        try {
            logger.info("StartBackup component[{}] with url[{}] body: {} ", component, urlComplete, backupBody);
            // We use a JsonNode to be more robust, in case the result change in a version. So, we can explore the JSON result.
            ResponseEntity<JsonNode> backupResponse = restTemplate.postForEntity(urlComplete, backupBody, JsonNode.class);

            backupOperation.status = backupResponse.getStatusCode().value();
            if (backupOperation.status != 200) {
                operationLog.error("backup [" + backupId + "] status[" + backupOperation.status + "] url [" + url + "]");
                return backupOperation;
            }
            // Extract the list of Snapshoot
            JsonNode bodyJson = backupResponse.getBody();
            if (bodyJson.has("scheduledSnapshots")) { // Check if scheduledSnapshots exists
                JsonNode snapshotsNode = bodyJson.get("scheduledSnapshots");

                if (snapshotsNode != null && snapshotsNode.isArray()) {
                    for (JsonNode node : snapshotsNode) {
                        backupOperation.listSnapshots.add(node.asText());
                    }
                }
                operationLog.info("backup [" + backupId + "] url[" + url + "] ListBackup[" + backupOperation.listSnapshots + "]");
            } else if (bodyJson.has("message")) { // Handle Optimize case where "message" exists
                backupOperation.message = bodyJson.get("message").asText();
                operationLog.info(String.format("backup [%d] url[%s] Message[%s]", backupId, url, backupOperation.message));
            } else {
                operationLog.warning(String.format("backup [%d] url[%s] Unexpected response: %s", backupId, url, bodyJson));
            }
        } catch (Exception e) {
            OperationException operationException = OperationException.getInstanceFromException(OperationException.BLUEBERRYERRORCODE.BACKUP, e);
            backupOperation.title = operationException.getError();
            backupOperation.message = "Component[" + component.toString() + "] Url[" + urlComplete + "]" + operationException.getMessage();
            operationLog.error("Component[" + component + "] backupId[" + backupId + "] url[" + url + "] Error[" + operationException.getMessage() + "]");
        }
        return backupOperation;
    }

    /**
     * Check the backup status
     *
     * @param component    component to communicate with
     * @param backupId     Backup ID
     * @param url          complete url to start the backup command in the component
     * @param operationLog log the operation
     */
    public void waitBackup(CamundaApplication.COMPONENT component, long backupId, String url,
                           OperationLog operationLog) {
        boolean written = false;
        ResponseEntity<ActuatorBackupStatusResponse> backupStatusResponse = null;
        do {
            logger.info("checking backup status for url {}", url);
            try {
                Thread.sleep(10_000L);
            } catch (InterruptedException e) {
                // do nothing
            }
            backupStatusResponse = restTemplate.getForEntity(url + "/actuator/backups/" + backupId, ActuatorBackupStatusResponse.class);
            logger.info("backup status response for url {}: {}, {}", url, backupStatusResponse.getStatusCodeValue(), backupStatusResponse.getBody());

            if (!written) {
                for (int i = 0; i < backupStatusResponse.getBody().getDetails().size(); i++) {
                    String csvSnapshotName = backupStatusResponse.getBody().getDetails().get(i).getSnapshotName();
                    operationLog.addSnapshotName(component.toString(), csvSnapshotName);
                }
                written = true; //only write once (this can surely be done better :-))
            }

        } while (backupStatusResponse.getStatusCode().is2xxSuccessful() && backupStatusResponse.getBody() != null && !backupStatusResponse.getBody().getState().equals("COMPLETED"));


    }


}
