package br.com.portalmanager.platform.messaging.exception;

import java.util.Map;

public class ApiException extends RuntimeException {
    private final String messageKey;
    private final Map<String, Object> parameters;

    public ApiException(String messageKey) {
        this(messageKey, Map.of(), null);
    }

    public ApiException(String messageKey, Throwable cause) {
        this(messageKey, Map.of(), cause);
    }

    public ApiException(String messageKey, Map<String, Object> parameters) {
        this(messageKey, parameters, null);
    }

    public ApiException(String messageKey, Map<String, Object> parameters, Throwable cause) {
        super(messageKey, cause); // Repassa a causa para o RuntimeException do Java
        this.messageKey = messageKey;
        this.parameters = parameters == null ? Map.of() : Map.copyOf(parameters);
    }

    public String getMessageKey() {
        return messageKey;
    }

    public Map<String, Object> getParameters() {
        return parameters;
    }
}
