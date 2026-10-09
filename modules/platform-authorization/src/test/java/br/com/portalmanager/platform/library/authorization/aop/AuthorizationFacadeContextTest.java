package br.com.portalmanager.platform.library.authorization.aop;

import br.com.portalmanager.platform.library.authorization.annotation.AuthorizationRequired;
import br.com.portalmanager.platform.library.authorization.client.AuthorizationClient;
import br.com.portalmanager.platform.library.authorization.config.PlatformAuthorizationProperties;
import br.com.portalmanager.platform.library.authorization.model.AuthorizationContext;
import br.com.portalmanager.platform.library.authorization.model.AuthorizationRequest;
import br.com.portalmanager.platform.library.authorization.model.UserContext;
import br.com.portalmanager.platform.library.authorization.model.UserSession;
import org.aspectj.lang.ProceedingJoinPoint;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.slf4j.MDC;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class AuthorizationFacadeContextTest {

    @AfterEach
    void cleanup() {
        UserContext.clear();
        MDC.clear();
    }

    @Test
    void realAspectRestoresContextAfterSuccess() throws Throwable {
        AuthorizationClient client = mock(AuthorizationClient.class);
        UserSession authorized = session("real");
        when(client.authorize(any(AuthorizationRequest.class))).thenReturn(authorized);
        AuthorizationFacadeAspect aspect = new AuthorizationFacadeAspect(client);
        assertRestoration((joinPoint, required) -> aspect.authorize(joinPoint, required), authorized);
    }

    @Test
    void realAspectRestoresContextAfterFailure() throws Throwable {
        AuthorizationClient client = mock(AuthorizationClient.class);
        UserSession authorized = session("real");
        when(client.authorize(any(AuthorizationRequest.class))).thenReturn(authorized);
        AuthorizationFacadeAspect aspect = new AuthorizationFacadeAspect(client);
        assertFailureRestoration((joinPoint, required) -> aspect.authorize(joinPoint, required));
    }

    @Test
    void mockAspectRestoresContextAfterSuccess() throws Throwable {
        MockAuthorizationFacadeAspect aspect = new MockAuthorizationFacadeAspect(new PlatformAuthorizationProperties());
        assertRestoration((joinPoint, required) -> aspect.authorize(joinPoint, required), null);
    }

    @Test
    void mockAspectRestoresContextAfterFailure() throws Throwable {
        MockAuthorizationFacadeAspect aspect = new MockAuthorizationFacadeAspect(new PlatformAuthorizationProperties());
        assertFailureRestoration((joinPoint, required) -> aspect.authorize(joinPoint, required));
    }

    private void assertRestoration(Invocation invocation, UserSession expected) throws Throwable {
        UserSession previous = session("previous");
        UserContext.set(previous);
        MDC.put("correlationId", "previous-id");
        MDC.put("custom", "preserve");
        ProceedingJoinPoint joinPoint = mock(ProceedingJoinPoint.class);
        when(joinPoint.getArgs()).thenReturn(new Object[]{context()});
        when(joinPoint.proceed()).thenAnswer(call -> {
            assertThat(UserContext.get()).isPresent();
            if (expected != null) assertThat(UserContext.get()).containsSame(expected);
            assertThat(MDC.get("correlationId")).isEqualTo("request-id");
            assertThat(MDC.get("custom")).isEqualTo("preserve");
            return "ok";
        });

        assertThat(invocation.call(joinPoint, mock(AuthorizationRequired.class))).isEqualTo("ok");
        assertThat(UserContext.get()).containsSame(previous);
        assertThat(MDC.get("correlationId")).isEqualTo("previous-id");
        assertThat(MDC.get("custom")).isEqualTo("preserve");
    }

    private void assertFailureRestoration(Invocation invocation) throws Throwable {
        UserSession previous = session("previous");
        UserContext.set(previous);
        MDC.put("correlationId", "previous-id");
        ProceedingJoinPoint joinPoint = mock(ProceedingJoinPoint.class);
        when(joinPoint.getArgs()).thenReturn(new Object[]{context()});
        when(joinPoint.proceed()).thenThrow(new IllegalStateException("business failure"));

        assertThatThrownBy(() -> invocation.call(joinPoint, mock(AuthorizationRequired.class)))
                .isInstanceOf(IllegalStateException.class).hasMessage("business failure");
        assertThat(UserContext.get()).containsSame(previous);
        assertThat(MDC.get("correlationId")).isEqualTo("previous-id");
        assertThat(MDC.get("username")).isNull();
    }

    private static AuthorizationContext context() {
        return new AuthorizationContext("request-id", "Bearer token", null, null, null);
    }

    private static UserSession session(String name) {
        UserSession session = new UserSession();
        session.setUserName(name);
        return session;
    }

    @FunctionalInterface
    private interface Invocation {
        Object call(ProceedingJoinPoint joinPoint, AuthorizationRequired required) throws Throwable;
    }
}
