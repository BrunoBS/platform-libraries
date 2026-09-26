package br.com.portalmanager.platform.library.authorization.web.filter;

import br.com.portalmanager.platform.library.authorization.model.UserContext;
import br.com.portalmanager.platform.library.authorization.model.UserSession;
import jakarta.servlet.ServletException;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.slf4j.MDC;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import java.io.IOException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class AuthorizationContextCleanupFilterTest {

    private final AuthorizationContextCleanupFilter filter = new AuthorizationContextCleanupFilter();

    @AfterEach
    void cleanup() {
        UserContext.clear();
        MDC.clear();
    }

    @Test
    void shouldClearContextAfterSuccessfulRequest() throws ServletException, IOException {
        UserSession session = new UserSession();
        session.setUserName("user");
        UserContext.set(session);
        MDC.put("correlationId", "corr-1");

        filter.doFilter(
                new MockHttpServletRequest(),
                new MockHttpServletResponse(),
                (request, response) -> {
                    UserContext.set(session);
                    MDC.put("correlationId", "corr-2");
                }
        );

        assertThat(UserContext.get()).isEmpty();
        assertThat(MDC.getCopyOfContextMap()).isNullOrEmpty();
    }

    @Test
    void shouldClearContextWhenRequestFails() {
        UserSession session = new UserSession();
        session.setUserName("user");

        assertThatThrownBy(() -> filter.doFilter(
                new MockHttpServletRequest(),
                new MockHttpServletResponse(),
                (request, response) -> {
                    UserContext.set(session);
                    MDC.put("correlationId", "corr-error");
                    throw new IllegalStateException("boom");
                }
        )).isInstanceOf(IllegalStateException.class);

        assertThat(UserContext.get()).isEmpty();
        assertThat(MDC.getCopyOfContextMap()).isNullOrEmpty();
    }
}
