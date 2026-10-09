package br.com.portalmanager.platform.library.authorization.model;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.slf4j.MDC;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class AuthorizationRequestContextTest {

    @AfterEach
    void cleanup() {
        UserContext.clear();
        MDC.clear();
    }

    @Test
    void shouldClearContextAfterSuccessfulRequest() {
        UserSession session = session("alice");
        try (AuthorizationRequestContext.Scope ignored = AuthorizationRequestContext.open(session, context("request-1"))) {
            assertThat(UserContext.get()).containsSame(session);
            assertThat(MDC.get("username")).isEqualTo("alice");
            assertThat(MDC.get("correlationId")).isEqualTo("request-1");
            assertThat(MDC.get("clientIp")).isEqualTo("127.0.0.1");
        }
        assertThat(UserContext.get()).isEmpty();
        assertThat(MDC.getCopyOfContextMap()).isNullOrEmpty();
    }

    @Test
    void shouldRestorePreviousContextAndEntireMdc() {
        UserSession previous = session("original");
        UserContext.set(previous);
        MDC.put("correlationId", "outer");
        MDC.put("custom", "keep");
        Map<String, String> originalMdc = MDC.getCopyOfContextMap();

        try (AuthorizationRequestContext.Scope ignored = AuthorizationRequestContext.open(session("temporary"), context("inner"))) {
            assertThat(UserContext.get().orElseThrow().getUserName()).isEqualTo("temporary");
            MDC.put("custom", "modified");
            MDC.put("unexpected", "value");
        }

        assertThat(UserContext.get()).containsSame(previous);
        assertThat(MDC.getCopyOfContextMap()).isEqualTo(originalMdc);
    }

    @Test
    void shouldRestoreContextWhenBusinessCodeThrows() {
        UserSession previous = session("original");
        UserContext.set(previous);
        MDC.put("correlationId", "outer");
        assertThatThrownBy(() -> {
            try (AuthorizationRequestContext.Scope ignored = AuthorizationRequestContext.open(session("temporary"), context("inner"))) {
                throw new IllegalStateException("failure");
            }
        }).isInstanceOf(IllegalStateException.class).hasMessage("failure");
        assertThat(UserContext.get()).containsSame(previous);
        assertThat(MDC.get("correlationId")).isEqualTo("outer");
        assertThat(MDC.get("username")).isNull();
    }

    @Test
    void shouldRestoreNestedScopesInReverseOrder() {
        UserSession outer = session("outer");
        UserSession inner = session("inner");
        try (AuthorizationRequestContext.Scope first = AuthorizationRequestContext.open(outer, context("outer-id"))) {
            try (AuthorizationRequestContext.Scope second = AuthorizationRequestContext.open(inner, context("inner-id"))) {
                assertThat(UserContext.get()).containsSame(inner);
                assertThat(MDC.get("correlationId")).isEqualTo("inner-id");
            }
            assertThat(UserContext.get()).containsSame(outer);
            assertThat(MDC.get("correlationId")).isEqualTo("outer-id");
        }
        assertThat(UserContext.get()).isEmpty();
        assertThat(MDC.getCopyOfContextMap()).isNullOrEmpty();
    }

    @Test
    void shouldNotLeakBetweenSequentialRequestsOnSameThread() {
        try (AuthorizationRequestContext.Scope ignored = AuthorizationRequestContext.open(session("first"), context("first-id"))) {
            assertThat(MDC.get("username")).isEqualTo("first");
        }
        assertThat(UserContext.get()).isEmpty();
        try (AuthorizationRequestContext.Scope ignored = AuthorizationRequestContext.open(session("second"), context("second-id"))) {
            assertThat(UserContext.get().orElseThrow().getUserName()).isEqualTo("second");
            assertThat(MDC.get("correlationId")).isEqualTo("second-id");
            assertThat(MDC.get("username")).isEqualTo("second");
        }
        assertThat(UserContext.get()).isEmpty();
        assertThat(MDC.getCopyOfContextMap()).isNullOrEmpty();
    }

    private static UserSession session(String name) {
        UserSession session = new UserSession();
        session.setUserName(name);
        session.setAccountId("account-" + name);
        return session;
    }

    private static AuthorizationContext context(String correlationId) {
        return new AuthorizationContext(correlationId, "Bearer token", null, null, null,
                "127.0.0.1", "JUnit", "/test");
    }
}
