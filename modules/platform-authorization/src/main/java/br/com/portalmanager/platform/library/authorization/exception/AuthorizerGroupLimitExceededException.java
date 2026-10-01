package br.com.portalmanager.platform.library.authorization.exception;

import br.com.portalmanager.platform.library.authorization.message.AuthorizationMessageKeys;

import java.util.Map;

public class AuthorizerGroupLimitExceededException extends AuthorizationException {
    public AuthorizerGroupLimitExceededException(int maxGroups) {
        super(AuthorizationMessageKeys.TOO_MANY_AUTHORIZER_GROUPS, Map.of("0", maxGroups));
    }
}
