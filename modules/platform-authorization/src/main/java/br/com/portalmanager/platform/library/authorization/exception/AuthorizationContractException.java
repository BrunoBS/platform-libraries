package br.com.portalmanager.platform.library.authorization.exception;

import br.com.portalmanager.platform.library.authorization.message.AuthorizationMessageKeys;

public class AuthorizationContractException extends AuthorizationException {
    public AuthorizationContractException(Throwable cause) {
        super(AuthorizationMessageKeys.SERVICE_CONTRACT_ERROR, cause);
    }
}
