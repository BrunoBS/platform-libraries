package br.com.portalmanager.platform.library.observability.logging.web;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.core.Ordered;
import org.springframework.web.filter.OncePerRequestFilter;
import org.springframework.web.util.ContentCachingRequestWrapper;

import java.io.IOException;

/**
 * Wraps the request early so its body can be read by observability components
 * at the exact moment an error log is emitted.
 *
 * <p>This filter deliberately does not write request data to MDC. Keeping the
 * payload attached to the request lifecycle prevents thread-local leakage
 * between requests.</p>
 */
public class PayloadErrorLoggingFilter extends OncePerRequestFilter implements Ordered {

    public static final int MAX_PAYLOAD_SIZE_BYTES = 1024 * 1024;

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain
    ) throws ServletException, IOException {
        ContentCachingRequestWrapper wrappedRequest =
                request instanceof ContentCachingRequestWrapper existing
                        ? existing
                        : new ContentCachingRequestWrapper(request, MAX_PAYLOAD_SIZE_BYTES);

        filterChain.doFilter(wrappedRequest, response);
    }

    @Override
    public int getOrder() {
        return Ordered.HIGHEST_PRECEDENCE + 5;
    }
}
