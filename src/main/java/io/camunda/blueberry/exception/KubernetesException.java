package io.camunda.blueberry.exception;

public class KubernetesException extends OperationException {
    /**
     * @param blueberryErrorCode the Blueberry code, categorize the error
     * @param status             Http Status, returned by Zeebe or any other API called
     * @param error              the error title
     * @param message            the detailled message
     */
    public KubernetesException(BLUEBERRYERRORCODE blueberryErrorCode, int status, String error, String message) {
        super(blueberryErrorCode, status, error, message);
    }
}
