package com.empresa.platform.audit.context;

import com.empresa.platform.audit.exception.AuditException;
import com.empresa.platform.audit.message.AuditMessageKeys;
import com.empresa.platform.audit.model.AuditContext;
import com.empresa.platform.authorization.model.UserContext;
import com.empresa.platform.authorization.model.UserSession;

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
