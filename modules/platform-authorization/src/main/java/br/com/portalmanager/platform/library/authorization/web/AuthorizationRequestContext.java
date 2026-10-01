package br.com.portalmanager.platform.library.authorization.web;

import br.com.portalmanager.platform.library.authorization.model.UserContext;
import br.com.portalmanager.platform.library.authorization.model.UserSession;
import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.MDC;

/**
 * Centralizes request-scoped authorization context and observability metadata.
 */
public final class AuthorizationRequestContext {

    private AuthorizationRequestContext() {
    }

    public static void set(UserSession session, HttpServletRequest request, String defaultUserAgent) {
        UserContext.set(session);

        String userAgent = request.getHeader("User-Agent");
        MDC.put("correlationId", session.getTraceId());
        MDC.put("username", session.getUserName());
        MDC.put("clientIp", request.getRemoteAddr());
        MDC.put("userAgent", userAgent != null ? userAgent : defaultUserAgent);
        MDC.put("uri", request.getRequestURI());
        MDC.put("accountId", session.getAccountId());
        MDC.put("environmentId", session.getEnvironmentId());
        MDC.put("applicationId", session.getApplicationId());
    }

    public static void clear() {
        UserContext.clear();
        MDC.clear();
    }
}
