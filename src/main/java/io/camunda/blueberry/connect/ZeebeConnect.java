package io.camunda.blueberry.connect;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.camunda.blueberry.connect.toolbox.WebActuator;
import io.camunda.blueberry.config.BlueberryConfig;
import io.camunda.blueberry.exception.OperationException;
import io.camunda.blueberry.operation.OperationLog;
import io.camunda.zeebe.client.ZeebeClient;
import io.camunda.zeebe.client.api.response.Topology;
import io.camunda.zeebe.protocol.impl.encoding.BackupStatusResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.StreamSupport;

@Component
public class ZeebeConnect extends WebActuator {
    private final ObjectMapper objectMapper;
    private final ZeebeClient zeebeClient;
    Logger logger = LoggerFactory.getLogger(ZeebeConnect.class);
    private final BlueberryConfig blueberryConfig;
    private final RestTemplate restTemplate;
    private ZeebeConnect zeebeConnect;

    public ZeebeConnect(BlueberryConfig blueberryConfig, RestTemplate restTemplate, ObjectMapper objectMapper, ZeebeClient zeebeClient) {
        super(restTemplate);
        this.blueberryConfig = blueberryConfig;
        this.restTemplate = restTemplate;
        this.objectMapper = objectMapper;
        this.zeebeClient = zeebeClient;
    }

    public void connection() {

    }

    /* ******************************************************************** */
    /*                                                                      */
    /*  getInformation                                                      */
    /*                                                                      */
    /* ******************************************************************** */
    public class ClusterInformation{
        public int clusterSize;
        public int partitionsCount;
        public int replicationFactor;
    }
    public ClusterInformation getClusterInformation() {
        final Topology topology = zeebeClient.newTopologyRequest().send().join();
        ClusterInformation clusterInformation = new ClusterInformation();
        clusterInformation.clusterSize= topology.getClusterSize();
        clusterInformation.partitionsCount= topology.getPartitionsCount();
        clusterInformation.replicationFactor= topology.getReplicationFactor();
        return clusterInformation;
    }

    /* ******************************************************************** */
    /*                                                                      */
    /*  Resume and pause                                                    */
    /*                                                                      */
    /* ******************************************************************** */

    public void pauseExporting(OperationLog operationLog) {
        ResponseEntity<String> pauseZeebeExporting = restTemplate.postForEntity(blueberryConfig.getZeebeActuatorUrl() + "/actuator/exporting/pause", new HashMap<>(), String.class);
        operationLog.info("Pause Zeebe exporting");
    }

    public void resumeExporting(OperationLog operationLog) {
        ResponseEntity<String> zeebeResumeResponse = restTemplate.postForEntity(blueberryConfig.getZeebeActuatorUrl() + "/actuator/exporting/resume", new HashMap<>(), String.class);
        operationLog.info("Resume Zeebe exporting");
    }

    /**
     * Get the exporter status
     * @return true: all exporter are ENABLED. False: one is disabled. null: impossible to say (list is empty)
     * @throws OperationException
     */
    public Boolean getExporterStatus() throws OperationException {
        try {
            ResponseEntity<JsonNode> listExporters = restTemplate.getForEntity(blueberryConfig.getZeebeActuatorUrl() + "/actuator/exporters", JsonNode.class);

            JsonNode listExportersNode = listExporters.getBody();
            if (listExportersNode != null && listExportersNode.isArray()) {
                boolean isRunning = true;
                for (JsonNode exporter : listExportersNode) {
                    String status = exporter.path("status").asText();
                    if ("DISABLED".equals(status))
                        isRunning = false;
                }
                return isRunning;
            }
            return null;
        } catch (Exception e) {
            throw OperationException.getInstanceFromException(OperationException.BLUEBERRYERRORCODE.STATUS_EXPORTER, e);
        }
    }
    /* ******************************************************************** */
    /*                                                                      */
    /*  Backup section                                                      */
    /*                                                                      */
    /* ******************************************************************** */

    public void backup(Long backupId, OperationLog operationLog) {
        Map<String, Object> backupBody = Map.of("backupId", backupId);
        ResponseEntity<String> backupResponse = restTemplate.postForEntity(blueberryConfig.getZeebeActuatorUrl() + "/actuator/backups", backupBody, String.class);

        logger.info("Backup status response for url {}: {}, {}", blueberryConfig.getZeebeActuatorUrl(), backupResponse.getStatusCodeValue(), backupResponse.getBody());

        if (backupResponse.getStatusCode().is2xxSuccessful()) {
            // Backup request was successfully scheduled
            logger.info("Backup {} successfully scheduled.", backupId);
        } else {
            // For any non-2xx status, log the error and resume exporting
            logger.error("Backup {} failed with status {}: {}", backupId, backupResponse.getStatusCode(), backupResponse.getBody());
            zeebeConnect.resumeExporting(operationLog);
        }
    }

    public void monitorBackup(Long backupId, OperationLog operationLog) {
        ObjectMapper objectMapper = new ObjectMapper();

        while (true) {
            logger.info("Checking backup status for URL {}", blueberryConfig.getZeebeActuatorUrl());
            try {
                Thread.sleep(10_000L);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                logger.warn("Thread interrupted while waiting", e);
                return;
            }

            ResponseEntity<String> response = restTemplate.getForEntity(
                    blueberryConfig.getZeebeActuatorUrl() + "/actuator/backups/" + backupId, String.class);

            logger.info("Backup status response for URL {}: {}, {}",
                    blueberryConfig.getZeebeActuatorUrl(), response.getStatusCodeValue(), response.getBody());

            if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
                try {
                    JsonNode root = objectMapper.readTree(response.getBody());
                    String state = root.path("state").asText();

                    if ("COMPLETED".equalsIgnoreCase(state)) {
                        logger.info("Backup {} is completed. Exiting loop.", backupId);
                        break;
                    }
                } catch (Exception e) {
                    logger.error("Failed to parse backup status response", e);
                }
            }
        }
    }

    public ZeebeInformation getInformation() {
        return new ZeebeInformation();
    }


    /* ******************************************************************** */
    /*                                                                      */
    /*  Administration                                                      */
    /*                                                                      */
    /* ******************************************************************** */

    /**
     * https://docs.camunda.io/docs/8.7/self-managed/operational-guides/backup-restore/zeebe-backup-and-restore/#list-backups-api
     */

    public List<BackupInfo> getListBackup() throws OperationException {
        ResponseEntity<BackupStatusResponse> backupStatusResponse = null;
        try {
            logger.info("Execute [{}]", blueberryConfig.getZeebeActuatorUrl() + "/actuator/backups");
            ResponseEntity<JsonNode> listResponse = restTemplate.getForEntity(blueberryConfig.getZeebeActuatorUrl() + "/actuator/backups", JsonNode.class);
            JsonNode jsonArray = listResponse.getBody();

            List<BackupInfo> listBackupInfo = StreamSupport.stream(jsonArray.spliterator(), false)
                    .map(t -> {
                        BackupInfo backupInfo = new BackupInfo();
                        backupInfo.backupId = t.get("backupId").asInt();
                        backupInfo.status = BackupInfo.fromZeebeStatus(t.get("state").asText());
                        // search the date in the first partition
                        JsonNode[] details = objectMapper.convertValue(t.get("details"), JsonNode[].class);

                        String timestamp = details[0].get("createdAt").asText();
                        LocalDateTime localDateTime = Instant.parse(timestamp)
                                .atZone(ZoneId.systemDefault()) // Convert to system's time zone
                                .toLocalDateTime();

                        backupInfo.backupTime = localDateTime;
                        return backupInfo;
                    }).toList();
            logger.info("Found {} backups", listBackupInfo.size());

            return listBackupInfo;
        } catch (Exception e) {
            throw OperationException.getInstanceFromException(OperationException.BLUEBERRYERRORCODE.BACKUP_LIST, e);
        }
    }

    public class ZeebeInformation {
        public int clusterSize;
        public int numberOfPartitions;
        public int replicaFactor;
    }
}
