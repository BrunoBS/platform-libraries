package br.com.portalmanager.platform.library.authorization.web;

import br.com.portalmanager.platform.library.authorization.config.PlatformAuthorizationProperties;
import br.com.portalmanager.platform.library.authorization.model.AuthorizationContext;
import br.com.portalmanager.platform.library.authorization.model.AuthorizationRequestContext;
import br.com.portalmanager.platform.library.authorization.model.UserSession;
import jakarta.servlet.DispatcherType;
import jakarta.servlet.ServletException;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.slf4j.MDC;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import java.io.IOException;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class AuthorizationRequestContextFilterTest {

    private AuthorizationRequestContextFilter filter;

    @BeforeEach
    void setUp() {
        MDC.clear();
        filter = new AuthorizationRequestContextFilter(new PlatformAuthorizationProperties());
    }

    @AfterEach
    void cleanUp() {
        MDC.clear();
    }

    @Test
    void exposesRequestMetadataToTheWholeDispatchAndRestoresPreviousMdc() throws Exception {
        MDC.put("correlationId", "stale-value");
        MDC.put("custom", "preserve-me");
        Map<String, String> previousMdc = MDC.getCopyOfContextMap();
        MockHttpServletRequest request = request("request-1");
        request.addHeader("authorization", "Bearer secret-token");
        request.addHeader("user-agent", "integration-test");

        filter.doFilter(request, new MockHttpServletResponse(), (filteredRequest, response) -> {
            assertThat(MDC.get("correlationId")).isEqualTo("request-1");
            assertThat(MDC.get("workspaceIdentifier")).isEqualTo("workspace-1");
            assertThat(MDC.get("applicationId")).isEqualTo("application-1");
            assertThat(MDC.get("environmentId")).isEqualTo("environment-1");
            assertThat(MDC.get("clientIp")).isEqualTo("192.0.2.10");
            assertThat(MDC.get("userAgent")).isEqualTo("integration-test");
            assertThat(MDC.get("uri")).isEqualTo("/api/v1/workspaces");
            assertThat(MDC.get("authorization")).isNull();
            assertThat(MDC.get("custom")).isEqualTo("preserve-me");
        });

        assertThat(MDC.getCopyOfContextMap()).isEqualTo(previousMdc);
    }

    @Test
    void removesStaleRequestValuesWhenTheNextRequestOmitsHeaders() throws Exception {
        MDC.put("correlationId", "previous-request");
        MDC.put("username", "previous-user");

        filter.doFilter(request(null), new MockHttpServletResponse(), (filteredRequest, response) -> {
            assertThat(MDC.get("correlationId")).isNull();
            assertThat(MDC.get("username")).isEqualTo("previous-user");
            assertThat(MDC.get("uri")).isEqualTo("/api/v1/workspaces");
        });

        assertThat(MDC.get("correlationId")).isEqualTo("previous-request");
        assertThat(MDC.get("username")).isEqualTo("previous-user");
    }

    @Test
    void restoresMdcWhenTheRequestChainThrows() {
        MDC.put("custom", "before-error");
        Map<String, String> previousMdc = MDC.getCopyOfContextMap();

        assertThatThrownBy(() -> filter.doFilter(request("error-request"), new MockHttpServletResponse(),
                (filteredRequest, response) -> {
                    assertThat(MDC.get("correlationId")).isEqualTo("error-request");
                    throw new ServletException("expected failure");
                }))
                .isInstanceOf(ServletException.class)
                .hasMessage("expected failure");

        assertThat(MDC.getCopyOfContextMap()).isEqualTo(previousMdc);
    }

    @Test
    void keepsRequestContextAvailableToErrorHandlingAfterAuthorizationScopeCloses() throws Exception {
        UserSession session = new UserSession();
        session.setUserName("alice");
        AuthorizationContext context = new AuthorizationContext(
                "request-1", "Bearer secret-token", null, null, null,
                "192.0.2.10", "integration-test", "/api/v1/workspaces");

        filter.doFilter(request("request-1"), new MockHttpServletResponse(), (filteredRequest, response) -> {
            try {
                try (AuthorizationRequestContext.Scope ignored = AuthorizationRequestContext.open(session, context)) {
                    assertThat(MDC.get("username")).isEqualTo("alice");
                    throw new IllegalArgumentException("expected business failure");
                }
            } catch (IllegalArgumentException expected) {
                assertThat(MDC.get("correlationId")).isEqualTo("request-1");
                assertThat(MDC.get("uri")).isEqualTo("/api/v1/workspaces");
                assertThat(MDC.get("username")).isNull();
            }
        });

        assertThat(MDC.getCopyOfContextMap()).isNullOrEmpty();
    }

    @Test
    void bindsAndRestoresMdcForAsyncAndErrorDispatches() throws Exception {
        for (DispatcherType dispatcherType : new DispatcherType[] {DispatcherType.ASYNC, DispatcherType.ERROR}) {
            MockHttpServletRequest request = request("dispatch-" + dispatcherType.name().toLowerCase());
            request.setDispatcherType(dispatcherType);

            filter.doFilter(request, new MockHttpServletResponse(), (filteredRequest, response) ->
                    assertThat(MDC.get("correlationId")).isEqualTo("dispatch-" + dispatcherType.name().toLowerCase()));

            assertThat(MDC.getCopyOfContextMap()).isNullOrEmpty();
        }
    }

    @Test
    void boundsAndSanitizesUntrustedRequestMetadata() throws Exception {
        MockHttpServletRequest request = request("bad\ncorrelation\tid");
        request.addHeader("user-agent", "agent\nwith-control");

        filter.doFilter(request, new MockHttpServletResponse(), (filteredRequest, response) -> {
            assertThat(MDC.get("correlationId")).isEqualTo("bad correlation id");
            assertThat(MDC.get("userAgent")).isEqualTo("agent with-control");
        });
    }

    private static MockHttpServletRequest request(String correlationId) {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setRequestURI("/api/v1/workspaces");
        request.setRemoteAddr("192.0.2.10");
        request.addHeader("workspace-identifier", "workspace-1");
        request.addHeader("application-identifier", "application-1");
        request.addHeader("environment-identifier", "environment-1");
        if (correlationId != null) {
            request.addHeader("correlation-id", correlationId);
        }
        return request;
    }
}
