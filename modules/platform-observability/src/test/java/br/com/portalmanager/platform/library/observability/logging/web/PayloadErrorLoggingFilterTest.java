package br.com.portalmanager.platform.library.observability.logging.web;

import jakarta.servlet.FilterChain;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.slf4j.MDC;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import static org.assertj.core.api.Assertions.assertThat;

class PayloadErrorLoggingFilterTest {

    private final PayloadErrorLoggingFilter filter = new PayloadErrorLoggingFilter();

    @AfterEach
    void tearDown() {
        MDC.clear();
    }

    @Test
    void shouldAddRequestBodyToMdcWhenResponseIsError() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setContentType("application/json");
        request.setContent("{\"name\":\"workspace\"}".getBytes());
        MockHttpServletResponse response = new MockHttpServletResponse();
        FilterChain chain = (req, res) -> {
            req.getInputStream().readAllBytes();
            ((MockHttpServletResponse) res).setStatus(400);
        };

        filter.doFilter(request, response, chain);

        assertThat(MDC.get("requestBody")).isEqualTo("{\"name\":\"workspace\"}");
    }

    @Test
    void shouldNotAddRequestBodyToMdcWhenResponseIsSuccessful() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setContent("payload".getBytes());
        MockHttpServletResponse response = new MockHttpServletResponse();
        FilterChain chain = (req, res) -> {
            req.getInputStream().readAllBytes();
            ((MockHttpServletResponse) res).setStatus(200);
        };

        filter.doFilter(request, response, chain);

        assertThat(MDC.get("requestBody")).isNull();
    }
}
