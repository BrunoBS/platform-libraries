package br.com.portalmanager.platform.library.authorization.web.filter;

import br.com.portalmanager.platform.library.authorization.web.AuthorizationRequestContext;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

/**
 * Guarantees request-scoped authorization context cleanup even when the MVC
 * interceptor pipeline fails before afterCompletion is invoked.
 */
public class AuthorizationContextCleanupFilter extends OncePerRequestFilter {

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain
    ) throws ServletException, IOException {

        // Defensive cleanup in case the container reuses a thread that still
        // contains state from a previously interrupted request.
        AuthorizationRequestContext.clear();

        try {
            filterChain.doFilter(request, response);
        } finally {
            UserContext.clear();
            MDC.clear();
        }
    }
}
