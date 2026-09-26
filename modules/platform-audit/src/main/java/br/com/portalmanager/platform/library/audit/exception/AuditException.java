package br.com.portalmanager.platform.library.audit.exception;

import br.com.portalmanager.platform.library.messaging.exception.ApiException;

public class AuditException extends ApiException {

    public AuditException(String messageKey) {
        super(messageKey);
    }

    public AuditException(String messageKey, Throwable cause) {
        super(messageKey, cause);
    }
}
