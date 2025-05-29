package io.camunda.blueberry.connect;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.camunda.blueberry.config.BlueberryConfig;
import io.camunda.blueberry.connect.toolbox.KubenetesToolbox;
import io.camunda.blueberry.connect.toolbox.WebActuator;
import io.camunda.blueberry.exception.BackupException;
import io.camunda.blueberry.operation.OperationLog;
import io.camunda.zeebe.client.api.response.Topology;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import java.util.Collections;
import java.util.List;

@Component
/**
 * Manage communication to OperateConnect
 */
public class OperateConnect implements CamundaApplication {
    Logger logger = LoggerFactory.getLogger(OperateConnect.class);


    private final BlueberryConfig blueberryConfig;
    private final WebActuator webActuator;
    private final KubenetesToolbox kubenetesToolbox;

    public OperateConnect(BlueberryConfig blueberryConfig, RestTemplate restTemplate) {
        webActuator = new WebActuator(restTemplate);
        kubenetesToolbox = new KubenetesToolbox();
        this.blueberryConfig = blueberryConfig;
    }

    public void connection() {

    }

    public boolean isConnected() {
        return webActuator.isConnected(COMPONENT.OPERATE, blueberryConfig.getOperateActuatorUrl()+"/actuator");
    }

    /**
     * Return the connection information plus information on the way to connect, in order to give back more feedback
     * @return
     */
    public CamundaApplication.ConnectionInfo isConnectedInformation() {
        return new CamundaApplication.ConnectionInfo(isConnected(),"Url Connection ["+blueberryConfig.getOperateActuatorUrl()+"/actuator]");
    }

    public COMPONENT getComponent() {
        return COMPONENT.OPERATE;
    }

    public boolean exist() {
        return kubenetesToolbox.isPodExist("operate");
    }


    public CamundaApplication.BackupOperation backup(Long backupId, OperationLog operationLog) throws BackupException {
        return webActuator.startBackup(CamundaApplication.COMPONENT.OPERATE, backupId, blueberryConfig.getOperateActuatorUrl(), operationLog);

    }


    public void waitBackup(Long backupId, OperationLog operationLog) {
        webActuator.waitBackup(CamundaApplication.COMPONENT.OPERATE, backupId, blueberryConfig.getOperateActuatorUrl(), operationLog);
    }

    /**
     * According to the documentation, Operate has a API to get all backup
     * https://docs.camunda.io/docs/8.7/self-managed/operational-guides/backup-restore/operate-tasklist-backup/#get-backups-list-api
     */
    public List<BackupInfo> getListBackup() {
        return Collections.emptyList();
    }

    public String getbackupRepositoryName() {
        try {
            RestTemplate restTemplate = new RestTemplate();

            // Call the /actuator/env endpoint and get the response as String
            String response = restTemplate.getForObject(blueberryConfig.getOperateActuatorUrl() + "/env", String.class);

            // Parse the JSON response
            ObjectMapper objectMapper = new ObjectMapper();
            JsonNode root = objectMapper.readTree(response);

            // The properties are under "propertySources" array
            JsonNode propertySources = root.path("propertySources");

            String targetVariable = "CAMUNDA_OPERATE_BACKUP_REPOSITORY_NAME";
            String value = null;

            // Iterate through propertySources to find the variable
            for (JsonNode source : propertySources) {
                JsonNode properties = source.path("properties");
                if (properties.has(targetVariable)) {
                    value = properties.get(targetVariable).path("value").asText();
                    break;
                }
            }
            return value;
        } catch (Exception e) {
            logger.error("Can't access actuator/env");
            return null;
        }
    }
}
