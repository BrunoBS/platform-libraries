package br.com.portalmanager.platform.library.observability.logging.web;

import jakarta.servlet.FilterChain;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.web.util.ContentCachingRequestWrapper;

import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.assertThat;

class PayloadErrorLoggingFilterTest {

    private final PayloadErrorLoggingFilter filter = new PayloadErrorLoggingFilter();

    @Test
    void shouldWrapRequestBeforeApplicationConsumesBody() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setContentType("application/json");
        request.setContent("{\"name\":\"workspace\"}".getBytes());
        MockHttpServletResponse response = new MockHttpServletResponse();
        AtomicReference<ContentCachingRequestWrapper> captured = new AtomicReference<>();

        FilterChain chain = (req, res) -> {
            ContentCachingRequestWrapper wrapper = (ContentCachingRequestWrapper) req;
            req.getInputStream().readAllBytes();
            captured.set(wrapper);
        };

        filter.doFilter(request, response, chain);

        assertThat(captured.get()).isNotNull();
        assertThat(new String(captured.get().getContentAsByteArray()))
                .isEqualTo("{\"name\":\"workspace\"}");
    }
}
