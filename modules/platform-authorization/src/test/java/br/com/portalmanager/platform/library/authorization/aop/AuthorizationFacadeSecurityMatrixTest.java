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

class AuthorizationFacadeSecurityMatrixTest {
    @AfterEach void cleanup() { UserContext.clear(); MDC.clear(); }

    @Test void forwardsEveryLevelAndActionIncludingOpen() {
        try (var context = new AnnotationConfigApplicationContext(Config.class)) {
            var facade = context.getBean(Facade.class);
            var client = context.getBean(AuthorizationClient.class);
            facade.open(valid());
            facade.dev(valid());
            facade.tst(valid());
            facade.adm(valid());
            facade.owner(valid());
            verify(client).authorize(argThat(r -> r.policy() == AuthorizationLevel.OPEN && r.action() == AuthorizationAction.READ));
            verify(client).authorize(argThat(r -> r.policy() == AuthorizationLevel.DEV && r.action() == AuthorizationAction.CREATE));
            verify(client).authorize(argThat(r -> r.policy() == AuthorizationLevel.TST && r.action() == AuthorizationAction.UPDATE));
            verify(client).authorize(argThat(r -> r.policy() == AuthorizationLevel.ADM && r.action() == AuthorizationAction.DELETE));
            verify(client).authorize(argThat(r -> r.policy() == AuthorizationLevel.OWNER && r.action() == AuthorizationAction.RESTORE));
            assertThat(facade.executionCount()).isEqualTo(5);
        }
    }

    @Test void nullSessionFailsClosed() {
        try (var context = new AnnotationConfigApplicationContext(Config.class)) {
            var facade = context.getBean(Facade.class);
            var client = context.getBean(AuthorizationClient.class);
            when(client.authorize(any())).thenReturn(null);
            assertThatThrownBy(() -> facade.open(valid())).isInstanceOf(RuntimeException.class);
            assertThat(facade.executionCount()).isZero();
            assertThat(UserContext.get()).isEmpty();
        }
    }

    @Test void remoteFailureFailsClosedForAllProtectedMethods() {
        try (var context = new AnnotationConfigApplicationContext(Config.class)) {
            var facade = context.getBean(Facade.class);
            var client = context.getBean(AuthorizationClient.class);
            when(client.authorize(any())).thenThrow(new IllegalStateException("remote unavailable"));
            assertThatThrownBy(() -> facade.open(valid())).isInstanceOf(IllegalStateException.class);
            assertThatThrownBy(() -> facade.dev(valid())).isInstanceOf(IllegalStateException.class);
            assertThatThrownBy(() -> facade.owner(valid())).isInstanceOf(IllegalStateException.class);
            assertThat(facade.executionCount()).isZero();
        }
    }

    @Test void openStillRequiresBearerToken() {
        try (var context = new AnnotationConfigApplicationContext(Config.class)) {
            var facade = context.getBean(Facade.class);
            var client = context.getBean(AuthorizationClient.class);
            assertThatThrownBy(() -> facade.open(new AuthorizationContext("id", "", null, null, null)))
                    .isInstanceOf(RuntimeException.class);
            verifyNoInteractions(client);
            assertThat(facade.executionCount()).isZero();
        }
    }

    private static AuthorizationContext valid() {
        return new AuthorizationContext("id", "Bearer token", null, null, null);
    }

    public static class Facade {
        int executions;
        public int executionCount() { return executions; }
        private void executed() { executions++; }
        @AuthorizationRequired(level = AuthorizationLevel.OPEN, action = AuthorizationAction.READ)
        public void open(AuthorizationContext context) { executed(); }
        @AuthorizationRequired(level = AuthorizationLevel.DEV, action = AuthorizationAction.CREATE)
        public void dev(AuthorizationContext context) { executed(); }
        @AuthorizationRequired(level = AuthorizationLevel.TST, action = AuthorizationAction.UPDATE)
        public void tst(AuthorizationContext context) { executed(); }
        @AuthorizationRequired(level = AuthorizationLevel.ADM, action = AuthorizationAction.DELETE)
        public void adm(AuthorizationContext context) { executed(); }
        @AuthorizationRequired(level = AuthorizationLevel.OWNER, action = AuthorizationAction.RESTORE)
        public void owner(AuthorizationContext context) { executed(); }
    }

    @Configuration(proxyBeanMethods = false)
    @EnableAspectJAutoProxy
    static class Config {
        @Bean AuthorizationClient client() {
            var client = mock(AuthorizationClient.class);
            var session = new UserSession();
            session.setUserName("authorized");
            when(client.authorize(any())).thenReturn(session);
            return client;
        }
        @Bean AuthorizationFacadeAspect aspect(AuthorizationClient client) {
            return new AuthorizationFacadeAspect(client);
        }
        @Bean Facade facade() { return new Facade(); }
    }
}
