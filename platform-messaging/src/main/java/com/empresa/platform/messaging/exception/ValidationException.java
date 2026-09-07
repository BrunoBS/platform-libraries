package com.empresa.platform.messaging.exception;

import java.util.Map;

public class ValidationException extends ApiException {

    public ValidationException(String messageKey) {
        super(messageKey);
    }

    public ValidationException(String messageKey, Throwable cause) {
        super(messageKey, cause);
    }

    public ValidationException(String messageKey, Map<String, Object> parameters) {
        super(messageKey, parameters);
    }

    public ValidationException(String messageKey, Map<String, Object> parameters, Throwable cause) {
        super(messageKey, parameters, cause);
    }
}
