package com.empresa.platform.audit.context;

import com.empresa.platform.audit.exception.AuditException;
import com.empresa.platform.audit.message.AuditMessageKeys;
import com.empresa.platform.audit.model.AuditContext;
import com.empresa.platform.authorization.model.UserContext;
import com.empresa.platform.authorization.model.UserSession;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class AuditAuthorizationContextResolverTest {

    private final AuditAuthorizationContextResolver resolver =
            new AuditAuthorizationContextResolver();

    @AfterEach
    void clearContext() {
        UserContext.clear();
    }

    @Test
    void shouldResolveAuditContextFromAuthorizedUserSession() {
        UserSession session = new UserSession();
        session.setUserName("bruno");
        session.setAccountId("account-1");
        session.setApplicationId("application-1");
        session.setEnvironmentId("dev");
        session.setTraceId("trace-1");
        UserContext.set(session);

        AuditContext context = resolver.resolve();

        assertThat(context.actor()).isEqualTo("bruno");
        assertThat(context.accountId()).isEqualTo("account-1");
        assertThat(context.applicationId()).isEqualTo("application-1");
        assertThat(context.environmentId()).isEqualTo("dev");
        assertThat(context.correlationId()).isEqualTo("trace-1");
    }

    @Test
    void shouldFailWhenAuthorizedUserContextIsMissing() {
        assertThatThrownBy(resolver::resolve)
                .isInstanceOf(AuditException.class)
                .extracting(exception -> ((AuditException) exception).getMessageKey())
                .isEqualTo(AuditMessageKeys.USER_CONTEXT_MISSING);
    }
}
