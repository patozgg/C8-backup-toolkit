package io.camunda.blueberry.connect;

import io.camunda.blueberry.connect.toolbox.KubenetesToolbox;
import io.camunda.blueberry.connect.toolbox.WebActuator;
import io.camunda.blueberry.config.BlueberryConfig;
import io.camunda.blueberry.exception.BackupException;
import io.camunda.blueberry.operation.OperationLog;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

@Component
public class OptimizeConnect implements CamundaApplication {

    private final BlueberryConfig blueberryConfig;
    private final RestTemplate restTemplate;
    private final WebActuator webActuator;
    private final KubenetesToolbox kubenetesToolbox;
    Logger logger = LoggerFactory.getLogger(OptimizeConnect.class.getName());

    OptimizeConnect(BlueberryConfig blueberryConfig, RestTemplate restTemplate) {
        webActuator = new WebActuator(restTemplate);
        kubenetesToolbox = new KubenetesToolbox();

        this.blueberryConfig = blueberryConfig;
        this.restTemplate = restTemplate;
    }

    public void connection() {
    }

    public boolean isConnected() {
        return webActuator.isConnected(COMPONENT.OPTIMIZE, blueberryConfig.getOptimizeActuatorUrl()+"/actuator");
    }

    /**
     * Return the connection information plus information on the way to connect, in order to give back more feedback
     * @return
     */
    public CamundaApplication.ConnectionInfo isConnectedInformation() {
        return new CamundaApplication.ConnectionInfo(isConnected(),"Url Connection ["+blueberryConfig.getOptimizeActuatorUrl()+"/actuator]");
    }

    public COMPONENT getComponent() {
        return COMPONENT.OPTIMIZE;
    }

    public boolean exist() {
        return kubenetesToolbox.isPodExist("optimize");
    }


    public BackupOperation backup(Long backupId, OperationLog operationLog) throws BackupException {
        return webActuator.startBackup(CamundaApplication.COMPONENT.OPTIMIZE, backupId, blueberryConfig.getOptimizeActuatorUrl(), operationLog);
    }

    public void waitBackup(Long backupId, OperationLog operationLog) {
        webActuator.waitBackup(CamundaApplication.COMPONENT.OPTIMIZE, backupId, blueberryConfig.getOptimizeActuatorUrl(), operationLog);
    }

}
