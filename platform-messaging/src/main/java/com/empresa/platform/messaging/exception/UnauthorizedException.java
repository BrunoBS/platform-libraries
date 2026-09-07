package com.empresa.platform.messaging.exception;

import java.util.Map;

public class UnauthorizedException extends ApiException {

    public UnauthorizedException(String messageKey) {
        super(messageKey);
    }

    public UnauthorizedException(String messageKey, Throwable cause) {
        super(messageKey, cause);
    }

    public UnauthorizedException(String messageKey, Map<String, Object> parameters) {
        super(messageKey, parameters);
    }

    public UnauthorizedException(String messageKey, Map<String, Object> parameters, Throwable cause) {
        super(messageKey, parameters, cause);
    }
}
