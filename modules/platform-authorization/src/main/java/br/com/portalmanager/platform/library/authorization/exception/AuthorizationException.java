package br.com.portalmanager.platform.library.authorization.exception;

import br.com.portalmanager.platform.library.messaging.exception.ApiException;

public abstract class AuthorizationException extends ApiException {

    protected AuthorizationException(String messageKey) {
        super(messageKey);
    }

    protected AuthorizationException(String messageKey, Throwable cause) {
        super(messageKey, cause);
    }

    /**
     * Backward-compatible alias for the authorization message key.
     */
    public String getCode() {
        return getMessageKey();
    }
}
