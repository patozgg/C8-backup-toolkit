package io.camunda.blueberry.exception;

import io.camunda.blueberry.connect.CamundaApplicationInt;

import java.util.HashMap;
import java.util.Map;

public class BackupException extends OperationException {

    private final Long backupId;
    private final CamundaApplicationInt.COMPONENT component;

    public BackupException(CamundaApplicationInt.COMPONENT component, int status, String error, String message, Long backupId) {
        super(BLUEBERRYERRORCODE.BACKUP, status, error, message);
        this.component = component;
        this.backupId = backupId;
    }

    public static BackupException getInstanceFromException(CamundaApplicationInt.COMPONENT component, Long backupId, Exception e) {
        OperationException operationException = getInstanceFromException(BLUEBERRYERRORCODE.BACKUP, e);
        return new BackupException(component, operationException.getStatus(), operationException.getError(), operationException.getMessage(), backupId);
    }

    public long getBackupId() {
        return backupId;
    }

    public CamundaApplicationInt.COMPONENT getComponent() {
        return component;
    }

    @Override
    public Map<String, Object> getRecord() {
        Map mapRecord = new HashMap<String, Object>();
        mapRecord.putAll(super.getRecord());
        mapRecord.put("backupId", backupId);
        mapRecord.put("component", component);
        return mapRecord;
    }


}