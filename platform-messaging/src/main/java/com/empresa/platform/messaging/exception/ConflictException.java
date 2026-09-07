package com.empresa.platform.messaging.exception;

import java.util.Map;

public class ConflictException extends ApiException {

    public ConflictException(String messageKey) {
        super(messageKey);
    }

    public ConflictException(String messageKey, Throwable cause) {
        super(messageKey, cause);
    }

    public ConflictException(String messageKey, Map<String, Object> parameters) {
        super(messageKey, parameters);
    }

    public ConflictException(String messageKey, Map<String, Object> parameters, Throwable cause) {
        super(messageKey, parameters, cause);
    }
}
