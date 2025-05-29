package io.camunda.blueberry.connect;

import com.fasterxml.jackson.databind.JsonNode;
import io.camunda.blueberry.config.BlueberryConfig;
import io.camunda.blueberry.connect.container.Container;
import io.camunda.blueberry.connect.container.ContainerFactory;
import io.camunda.blueberry.exception.ElasticsearchException;
import io.camunda.blueberry.operation.OperationLog;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.*;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;

import java.util.List;
import java.util.Map;

@Component
public class ElasticSearchConnect {
    Logger logger = LoggerFactory.getLogger(ElasticSearchConnect.class);

    BlueberryConfig blueberryConfig;

    ContainerFactory containerFactory;

    @Autowired
    private RestTemplate restTemplate; // Injected instance

    public ElasticSearchConnect(BlueberryConfig blueberryConfig, ContainerFactory containerFactory) {
        this.blueberryConfig = blueberryConfig;
        this.containerFactory = containerFactory;
    }

    public OperationResult connection() {
        OperationResult operationResult = new OperationResult();
        operationResult.success = true;
        return operationResult;
    }

    public boolean isConnected() {
        String url = blueberryConfig.getElasticsearchUrl();
        try {
            ResponseEntity<JsonNode> response = restTemplate.getForEntity(url, JsonNode.class);
            // receive {"operaterepository":{"type":"azure","uuid":"7BlNbbw3TxadAqc6M523cw","settings":{"container":"elasticsearchcontainer","base_path":"operatebackup"}}}
            if (response.getStatusCode() != HttpStatus.OK) {
                logger.error("Elasticsearch connection failed with status " + response.getStatusCode());
                return false;
            }
            logger.info("Elasticsearch connection successful");
            return true;
        } catch (Exception e) {
            logger.error("Can't access ElasticSearch url[{}] {} ", url, e);
            return false;
        }
    }

    /**
     * Return the connection information plus informatin on the way to connect, in order to give back more feedback
     *
     * @return
     */
    public CamundaApplication.ConnectionInfo isConnectedInformation() {
        return new CamundaApplication.ConnectionInfo(isConnected(), "Url Connection [" + blueberryConfig.getElasticsearchUrl() + "]");
    }

    public OperationResult existRepository(String repositoryName) {
        OperationResult operationResult = new OperationResult();
        String url = blueberryConfig.getElasticsearchUrl() + "/_snapshot/" + repositoryName;
        operationResult.command = "PUT " + url;

        try {
            // ResponseEntity<String> response = restTemplate.getForEntity(operationResult.command, String.class);
            ResponseEntity<JsonNode> response = restTemplate.getForEntity(url, JsonNode.class);
            // receive {"operaterepository":{"type":"azure","uuid":"7BlNbbw3TxadAqc6M523cw","settings":{"container":"elasticsearchcontainer","base_path":"operatebackup"}}}
            if (response.getStatusCode() != HttpStatus.OK) {
                operationResult.success = false;
                operationResult.details = "Can't connect to ElasticSearch [" + operationResult.command + "]: Http " + response.getStatusCode().value();
                return operationResult;
            }

            JsonNode jsonNode = response.getBody();
// is a repository is defined?
            String containerType = jsonNode==null? null: jsonNode.path("operaterepository").path("type").asText();
            String containerUuid = jsonNode==null? null: jsonNode.path("operaterepository").path("uuid").asText();
            operationResult.details = " ContainerType[" + containerType + "] uuid[" + containerUuid + "]";

            operationResult.resultBoolean = true;
            operationResult.success = true;

            return operationResult;
        } catch (HttpClientErrorException e) {
            if (e.getStatusCode() == HttpStatus.NOT_FOUND) {
                operationResult.success = false;
                operationResult.details = "Repository [" + repositoryName + "] does not exist";
            } else {
                operationResult.success = false;
                operationResult.details = e.getMessage();
            }
            return operationResult; // If an exception occurs (e.g., 404 error), assume the repository doesn't exist
        } catch (Exception e) {
            operationResult.success = false;
            operationResult.details = e.getMessage();
            return operationResult; // If an exception occurs (e.g., 404 error), assume the repository doesn't exist
        }
    }

    /**
     * Create a repository with the command
     * <p>
     * To delete it: curl -X DELETE "http://localhost:9200/_snapshot/operaterepository"
     *
     * @param repositoryName
     * @param containerType           azure, S3...
     * @param basePathInsideContainer inside the container, the path
     * @throws ElasticsearchException exception in case of error
     */
    public OperationResult createRepository(String repositoryName,
                                            String containerType,
                                            String basePathInsideContainer) {
        OperationResult operationResult = new OperationResult();
        operationResult.command = blueberryConfig.getElasticsearchUrl() + "/_snapshot/" + repositoryName;
        Container container = containerFactory.getContainerFromType(containerType);
        if (container == null) {
            operationResult.success = false;
            operationResult.details = "Can't find container [" + containerType + "]";
            return operationResult;
        }

        String jsonPayload = container.getElasticsearchPayload(basePathInsideContainer);

        try {
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);

            // Create HTTP entity with payload and headers
            HttpEntity<String> requestEntity = new HttpEntity<>(jsonPayload, headers);

            // Make PUT request
            ResponseEntity<String> response = restTemplate.exchange(operationResult.command, HttpMethod.PUT, requestEntity, String.class);

            if (!response.getStatusCode().is2xxSuccessful()) {
                operationResult.success = false;
                operationResult.details = "Can't create repository : code["
                        + response.getStatusCode().value() + " RepositoryName[" + repositoryName
                        + "] using container information[" + container.getInformation()
                        + "] Type[" + containerType + "] path[" + basePathInsideContainer + "] " + response.getBody();
                return operationResult;
            }

            operationResult.success = true;
            return operationResult;

        } catch (Exception e) {
            operationResult.success = false;
            operationResult.details = "Can't create repository : RepositoryName[" + repositoryName
                    + "] using container name[" + container.getInformation()
                    + "] Type[" + containerType + "] path[" + basePathInsideContainer + "] : "
                    + e.getMessage();
            return operationResult;
        }

    }

    /**
     * curl -X PUT http://localhost:9200/_snapshot/zeeberecordrepository/12 -H 'Content-Type: application/json'   \
     * -d '{ "indices": "zeebe-record*", "feature_states": ["none"]}'
     *
     * @param backupId
     * @param operationLog
     */
    public void esBackup(Long backupId, OperationLog operationLog) {
        String zeebeEsRepository = blueberryConfig.getZeebeRecordRepository();

        HttpEntity<?> zeebeEsBackupRequest = new HttpEntity<>(Map.of("indices", "zeebe-record*", "feature_states", List.of("none")));
        ResponseEntity<String> zeebeEsBackupResponse = restTemplate.exchange(blueberryConfig.getElasticsearchUrl() + "/_snapshot/" + blueberryConfig.getZeebeRepository() + "/" + backupId, HttpMethod.PUT, zeebeEsBackupRequest, String.class);
        operationLog.info("Start Zeebe ES Backup on [" + zeebeEsRepository + "] response: " + zeebeEsBackupResponse.getStatusCode().value() + " [" + zeebeEsBackupResponse.getBody() + "]");
    }
}
