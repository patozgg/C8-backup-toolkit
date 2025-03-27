package io.camunda.blueberry.connect;

import java.util.Map;

public class OperationResult {
    public boolean success;
    public String command;
    public String details;


    /**
     * if the result is a String, this contains it
     */
    public String resultSt;
    public boolean resultBoolean;
    public Map<String, Object> resultMap;
}
