package br.com.portalmanager.platform.library.authorization.web.filter;

import jakarta.servlet.ServletException;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.slf4j.MDC;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

import static org.assertj.core.api.Assertions.assertThat;

class PayloadErrorLoggingFilterTest {

    private final PayloadErrorLoggingFilter filter = new PayloadErrorLoggingFilter();

    @AfterEach
    void cleanup() {
        MDC.clear();
    }

    @Test
    void shouldMaskSensitiveJsonFieldsWhenRequestFails() throws ServletException, IOException {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setContentType("application/json");
        request.setContent("""
                {"username":"bruno","password":"secret-value","token":"jwt-value","client_secret":"client-value"}
                """.getBytes(StandardCharsets.UTF_8));

        MockHttpServletResponse response = new MockHttpServletResponse();

        filter.doFilter(request, response, (wrappedRequest, wrappedResponse) -> {
            wrappedRequest.getInputStream().readAllBytes();
            ((jakarta.servlet.http.HttpServletResponse) wrappedResponse).setStatus(400);
        });

        assertThat(MDC.get("requestBody"))
                .contains("\\"username\\":\\"bruno\\"")
                .contains("\\"password\\":\\"***\\"")
                .contains("\\"token\\":\\"***\\"")
                .contains("\\"client_secret\\":\\"***\\"")
                .doesNotContain("secret-value", "jwt-value", "client-value");
    }

    @Test
    void shouldNotStorePayloadForSuccessfulRequest() throws ServletException, IOException {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setContent("{}".getBytes(StandardCharsets.UTF_8));
        MockHttpServletResponse response = new MockHttpServletResponse();

        filter.doFilter(request, response, (wrappedRequest, wrappedResponse) -> {
            wrappedRequest.getInputStream().readAllBytes();
            ((jakarta.servlet.http.HttpServletResponse) wrappedResponse).setStatus(200);
        });

        assertThat(MDC.get("requestBody")).isNull();
    }
}
