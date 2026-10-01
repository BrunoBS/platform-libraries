package br.com.portalmanager.platform.library.observability.logging.web;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.core.Ordered;
import org.springframework.web.filter.OncePerRequestFilter;
import org.springframework.web.util.ContentCachingRequestWrapper;

import java.io.IOException;

public class PayloadErrorLoggingFilter extends OncePerRequestFilter implements Ordered {

    public static final int DEFAULT_MAX_PAYLOAD_SIZE_BYTES = 1024 * 1024;

    private final int maxPayloadSizeBytes;

    public PayloadErrorLoggingFilter(int maxPayloadSizeBytes) {
        if (maxPayloadSizeBytes <= 0) {
            throw new IllegalArgumentException("Request body max size must be greater than zero");
        }
        this.maxPayloadSizeBytes = maxPayloadSizeBytes;
    }

    public int getMaxPayloadSizeBytes() {
        return maxPayloadSizeBytes;
    }

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain
    ) throws ServletException, IOException {
        ContentCachingRequestWrapper wrappedRequest =
                request instanceof ContentCachingRequestWrapper existing
                        ? existing
                        : new ContentCachingRequestWrapper(request, maxPayloadSizeBytes);

        wrappedRequest.setAttribute(PayloadErrorLoggingFilter.class.getName() + ".maxPayloadSizeBytes", maxPayloadSizeBytes);
        filterChain.doFilter(wrappedRequest, response);
    }

    @Override
    public int getOrder() {
        return Ordered.HIGHEST_PRECEDENCE + 5;
    }
}
