package com.empresa.platform.audit.exception;

import com.empresa.platform.messaging.exception.ApiException;

public class AuditException extends ApiException {

    public AuditException(String messageKey) {
        super(messageKey);
    }

    public AuditException(String messageKey, Throwable cause) {
        super(messageKey, cause);
    }
}
