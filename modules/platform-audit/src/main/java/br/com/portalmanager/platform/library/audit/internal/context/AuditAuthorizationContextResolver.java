package br.com.portalmanager.platform.library.audit.internal.context;

import br.com.portalmanager.platform.library.messaging.api.exception.ValidationException;
import br.com.portalmanager.platform.library.audit.internal.message.AuditMessageKeys;
import br.com.portalmanager.platform.library.audit.api.contract.model.AuditContext;
import br.com.portalmanager.platform.library.authorization.api.model.UserContext;
import br.com.portalmanager.platform.library.authorization.api.model.UserSession;

public final class AuditAuthorizationContextResolver {

    public AuditContext resolve() {
        return UserContext.get()
                .map(this::fromSession)
                .orElseThrow(() -> new ValidationException(
                        AuditMessageKeys.USER_CONTEXT_MISSING
                ));
    }

    private AuditContext fromSession(UserSession session) {
        return new AuditContext(
                session.getAccountId(),
                session.getApplicationId(),
                session.getEnvironmentId(),
                session.getUserName(),
                session.getTraceId()
        );
    }
}
