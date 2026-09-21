package br.com.portalmanager.platform.messaging.exception;

import java.util.Map;

public class ForbiddenException extends ApiException {

    public ForbiddenException(String messageKey) {
        super(messageKey);
    }

    public ForbiddenException(String messageKey, Throwable cause) {
        super(messageKey, cause);
    }

    public ForbiddenException(String messageKey, Map<String, Object> parameters) {
        super(messageKey, parameters);
    }

    public ForbiddenException(String messageKey, Map<String, Object> parameters, Throwable cause) {
        super(messageKey, parameters, cause);
    }
}
