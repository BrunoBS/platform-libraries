package br.com.portalmanager.core.audit.exception;

import br.com.portalmanager.core.messaging.exception.ApiException;

public class AuditException extends ApiException {

    public AuditException(String messageKey) {
        super(messageKey);
    }

    public AuditException(String messageKey, Throwable cause) {
        super(messageKey, cause);
    }
}
