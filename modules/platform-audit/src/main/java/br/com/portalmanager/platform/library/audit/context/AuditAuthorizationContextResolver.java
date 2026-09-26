package br.com.portalmanager.platform.library.audit.context;

import br.com.portalmanager.platform.library.audit.exception.AuditException;
import br.com.portalmanager.platform.library.audit.message.AuditMessageKeys;
import br.com.portalmanager.platform.library.audit.model.AuditContext;
import br.com.portalmanager.platform.library.authorization.model.UserContext;
import br.com.portalmanager.platform.library.authorization.model.UserSession;

public final class AuditAuthorizationContextResolver {

    public AuditContext resolve() {
        return UserContext.get()
                .map(this::fromSession)
                .orElseThrow(() -> new AuditException(
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
