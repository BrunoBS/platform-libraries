package br.com.portalmanager.core.messaging.exception;

import java.util.Map;

public class NotFoundException extends ApiException {

    public NotFoundException(String messageKey) {
        super(messageKey);
    }

    public NotFoundException(String messageKey, Throwable cause) {
        super(messageKey, cause);
    }

    public NotFoundException(String messageKey, Map<String, Object> parameters) {
        super(messageKey, parameters);
    }

    public NotFoundException(String messageKey, Map<String, Object> parameters, Throwable cause) {
        super(messageKey, parameters, cause);
    }
}
