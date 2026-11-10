package br.com.portalmanager.platform.library.authorization.api.exception;

import br.com.portalmanager.platform.library.authorization.internal.message.AuthorizationMessageKeys;

public class AuthorizationContractException extends AuthorizationException {
    public AuthorizationContractException(Throwable cause) {
        super(AuthorizationMessageKeys.SERVICE_CONTRACT_ERROR, cause);
    }
}
