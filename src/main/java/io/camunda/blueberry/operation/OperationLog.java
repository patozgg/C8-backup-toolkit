package io.camunda.blueberry.operation;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;

public class OperationLog {
    private final List<Message> listMessages = new ArrayList<>();
    Logger logger = LoggerFactory.getLogger(OperationLog.class);
    Map<String, List<String>> snapshotPerComponents = new HashMap<>();
    private String operationName;
    private int totalNumberOfSteps;
    private int currentStep;
    private String stepName;
    private long operationBeginTime;

    public OperationLog() {
    }

    /**
     * Start an operation
     *
     * @param operationName      the operation name
     * @param totalNumberOfSteps total number of step expected
     */
    public void startOperation(String operationName, int totalNumberOfSteps) {
        info("Start operation [" + operationName + "] with " + totalNumberOfSteps + " steps");
        this.operationName = operationName;
        this.totalNumberOfSteps = totalNumberOfSteps;
        this.currentStep = 0;
        this.operationBeginTime = System.currentTimeMillis();
    }

    /**
     * a new step is started
     *
     * @param stepName name of the step
     */
    public void operationStep(String stepName) {
        this.currentStep++;
        this.stepName= stepName;
        info("Operation[" + operationName + "/" + stepName + "] : " + currentStep + "/" + totalNumberOfSteps);
    }

    public void operationStep(int forceStep, String stepName) {
        this.currentStep=forceStep;
        this.stepName= stepName;
        info("Operation[" + operationName + "/" + stepName + "] : " + currentStep + "/" + totalNumberOfSteps);
    }

    public void endOperation() {
        info("Operation[" + operationName + "] : finished in " + (System.currentTimeMillis() - operationBeginTime) + " ms");
    }

    public void info(String message) {
        logger.info(message);
        Message msg = new Message();
        msg.type = Type.INFO;
        msg.message = message;
        msg.date = new Date();
        listMessages.add(msg);
    }

    public void warning(String message) {
        logger.error(message);
        Message msg = new Message();
        msg.type = Type.WARNING;
        msg.message = message;
        msg.date = new Date();
        listMessages.add(msg);
    }

    public void error(String message) {
        logger.error(message);
        Message msg = new Message();
        msg.type = Type.ERROR;
        msg.message = message;
        msg.date = new Date();
        listMessages.add(msg);
    }

    public List<Message> getMessages() {
        return listMessages;
    }

    public void addSnapshotName(String component, String snapshotName) {
        List<String> listSnapshop = snapshotPerComponents.get(component);
        if (listSnapshop == null) {
            listSnapshop = new ArrayList();
        }
        listSnapshop.add(snapshotName);
        snapshotPerComponents.put(component, listSnapshop);
    }

    public String getOperationName() {
        return operationName;
    }

    public int getTotalNumberOfSteps() {
        return totalNumberOfSteps;
    }

    public int getCurrentStep() {
        return currentStep;
    }

    public String getStepName() {
        return stepName;
    }

    enum Type {INFO, WARNING, ERROR}

    public class Message {
        public Type type;
        public String message;
        public Date date;
    }
}
