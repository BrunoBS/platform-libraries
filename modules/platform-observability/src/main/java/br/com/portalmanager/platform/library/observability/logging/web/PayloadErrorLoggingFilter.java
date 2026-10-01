package br.com.portalmanager.platform.library.observability.logging.web;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.MDC;
import org.springframework.core.Ordered;
import org.springframework.web.filter.OncePerRequestFilter;
import org.springframework.web.util.ContentCachingRequestWrapper;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

public class PayloadErrorLoggingFilter extends OncePerRequestFilter implements Ordered {

    private static final String MDC_REQUEST_BODY_KEY = "requestBody";
    private static final int MAX_PAYLOAD_SIZE_BYTES = 1024 * 1024;

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {

        ContentCachingRequestWrapper wrappedRequest =
                new ContentCachingRequestWrapper(request, MAX_PAYLOAD_SIZE_BYTES);
        try {
            filterChain.doFilter(wrappedRequest, response);
        } finally {
            if (response.getStatus() >= 400) {
                String body = getPayloadFromBody(wrappedRequest);
                if (!body.isBlank()) {
                    MDC.put(MDC_REQUEST_BODY_KEY, body);
                }
            }
        }
    }

    private String getPayloadFromBody(ContentCachingRequestWrapper request) {
        byte[] buffer = request.getContentAsByteArray();
        if (buffer.length == 0) {
            return "";
        }
        try {
            boolean truncated = buffer.length >= MAX_PAYLOAD_SIZE_BYTES;
            String content = new String(buffer, StandardCharsets.UTF_8)
                    .replaceAll("\\s+", " ")
                    .trim();
            return truncated
                    ? content + " ... [PAYLOAD TRUNCATED - EXCEEDED 1MB LIMIT]"
                    : content;
        } catch (Exception ignored) {
            return "[unreadable-payload]";
        }
    }

    @Override
    public int getOrder() {
        return Ordered.HIGHEST_PRECEDENCE + 5;
    }
}
