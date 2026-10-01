package br.com.portalmanager.platform.library.authorization.exception;

import br.com.portalmanager.platform.library.authorization.message.AuthorizationMessageKeys;

/**
 * Signals a technical failure while communicating with the central Authorization API.
 */
public class AuthorizationServiceUnavailableException extends AuthorizationException {

    public AuthorizationServiceUnavailableException(Throwable cause) {
        super(AuthorizationMessageKeys.SERVICE_UNAVAILABLE, cause);
    }
}
