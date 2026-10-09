package br.com.portalmanager.platform.library.authorization.aop;

import br.com.portalmanager.platform.library.authorization.annotation.AuthorizationRequired;
import br.com.portalmanager.platform.library.authorization.client.AuthorizationClient;
import br.com.portalmanager.platform.library.authorization.model.*;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.slf4j.MDC;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.EnableAspectJAutoProxy;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class AuthorizationFacadeProxyIntegrationTest {
    @AfterEach void clean() { UserContext.clear(); MDC.clear(); }

    @Test void publicFacadeMethodIsAuthorizedThroughSpringProxy() {
        try (var context = new AnnotationConfigApplicationContext(TestConfig.class)) {
            var facade = context.getBean(SecuredFacade.class);
            var client = context.getBean(AuthorizationClient.class);
            assertThat(facade.read(valid())).isEqualTo("alice");
            verify(client).authorize(argThat(request ->
                    request.action() == AuthorizationAction.READ &&
                    request.policy() == AuthorizationLevel.OPEN));
            assertThat(UserContext.get()).isEmpty();
            assertThat(MDC.getCopyOfContextMap()).isNullOrEmpty();
        }
    }

    @Test void missingCredentialsFailClosedWithoutCallingBusinessMethod() {
        try (var context = new AnnotationConfigApplicationContext(TestConfig.class)) {
            var facade = context.getBean(SecuredFacade.class);
            var client = context.getBean(AuthorizationClient.class);
            assertThatThrownBy(() -> facade.read(new AuthorizationContext("id", null, null, null, null)))
                    .isInstanceOf(RuntimeException.class);
            verifyNoInteractions(client);
            assertThat(UserContext.get()).isEmpty();
        }
    }

    @Test void deniedAuthorizationNeverExecutesBusinessMethod() {
        try (var context = new AnnotationConfigApplicationContext(TestConfig.class)) {
            var facade = context.getBean(SecuredFacade.class);
            var client = context.getBean(AuthorizationClient.class);
            reset(client);
            when(client.authorize(any())).thenThrow(new IllegalStateException("denied"));
            assertThatThrownBy(() -> facade.read(valid())).isInstanceOf(IllegalStateException.class);
            assertThat(UserContext.get()).isEmpty();
        }
    }

    @Test void businessExceptionRestoresPreviousContext() {
        try (var context = new AnnotationConfigApplicationContext(TestConfig.class)) {
            var facade = context.getBean(SecuredFacade.class);
            UserSession previous = new UserSession();
            previous.setUserName("previous");
            UserContext.set(previous);
            MDC.put("correlationId", "previous-id");
            assertThatThrownBy(() -> facade.fail(valid())).isInstanceOf(IllegalStateException.class);
            assertThat(UserContext.get()).containsSame(previous);
            assertThat(MDC.get("correlationId")).isEqualTo("previous-id");
        }
    }

    @Test void selfInvocationDoesNotTriggerSecondAuthorizationAndMustNotBeReliedOn() {
        try (var context = new AnnotationConfigApplicationContext(TestConfig.class)) {
            var facade = context.getBean(SecuredFacade.class);
            var client = context.getBean(AuthorizationClient.class);
            assertThat(facade.outer(valid())).isEqualTo("alice");
            verify(client, times(1)).authorize(any());
        }
    }

    private static AuthorizationContext valid() {
        return new AuthorizationContext("request-id", "Bearer token", null, null, null);
    }

    public static class SecuredFacade {
        @AuthorizationRequired(level = AuthorizationLevel.OPEN, action = AuthorizationAction.READ)
        public String read(AuthorizationContext input) {
            return UserContext.get().orElseThrow().getUserName();
        }
        @AuthorizationRequired(level = AuthorizationLevel.DEV, action = AuthorizationAction.UPDATE)
        public String fail(AuthorizationContext input) { throw new IllegalStateException("business"); }
        @AuthorizationRequired(level = AuthorizationLevel.DEV, action = AuthorizationAction.READ)
        public String outer(AuthorizationContext input) { return read(input); }
    }

    @Configuration(proxyBeanMethods = false)
    @EnableAspectJAutoProxy
    static class TestConfig {
        @Bean AuthorizationClient authorizationClient() {
            AuthorizationClient client = mock(AuthorizationClient.class);
            UserSession session = new UserSession();
            session.setUserName("alice");
            when(client.authorize(any())).thenReturn(session);
            return client;
        }
        @Bean AuthorizationFacadeAspect authorizationFacadeAspect(AuthorizationClient client) {
            return new AuthorizationFacadeAspect(client);
        }
        @Bean SecuredFacade securedFacade() { return new SecuredFacade(); }
    }
}
