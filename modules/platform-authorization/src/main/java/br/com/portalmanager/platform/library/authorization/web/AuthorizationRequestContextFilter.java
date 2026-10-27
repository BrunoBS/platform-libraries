package br.com.portalmanager.platform.library.authorization.web;

import br.com.portalmanager.platform.library.authorization.config.PlatformAuthorizationProperties;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.MDC;
import org.springframework.core.Ordered;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.List;
import java.util.Map;

/** Binds safe HTTP request metadata to the MDC for the full servlet dispatch. */
public final class AuthorizationRequestContextFilter extends OncePerRequestFilter implements Ordered {

    private static final int MAX_IDENTIFIER_LENGTH = 128;
    private static final int MAX_USER_AGENT_LENGTH = 512;
    private static final int MAX_URI_LENGTH = 2048;
    private static final int MAX_CLIENT_IP_LENGTH = 128;

    private static final List<String> REQUEST_MDC_KEYS = List.of(
            "correlationId", "workspaceIdentifier", "applicationId", "environmentId",
            "clientIp", "userAgent", "uri");

    private final PlatformAuthorizationProperties.Headers headers;

    public AuthorizationRequestContextFilter(PlatformAuthorizationProperties properties) {
        this.headers = properties.getHeaders();
    }

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain
    ) throws ServletException, IOException {
        Map<String, String> previousMdc = MDC.getCopyOfContextMap();
        try {
            clearRequestKeys();
            put("correlationId", header(request, headers.getCorrelationId()), MAX_IDENTIFIER_LENGTH);
            put("workspaceIdentifier", header(request, headers.getWorkspaceIdentifier()), MAX_IDENTIFIER_LENGTH);
            put("applicationId", header(request, headers.getApplicationIdentifier()), MAX_IDENTIFIER_LENGTH);
            put("environmentId", header(request, headers.getEnvironmentIdentifier()), MAX_IDENTIFIER_LENGTH);
            put("clientIp", request.getRemoteAddr(), MAX_CLIENT_IP_LENGTH);
            put("userAgent", request.getHeader("user-agent"), MAX_USER_AGENT_LENGTH);
            put("uri", request.getRequestURI(), MAX_URI_LENGTH);
            filterChain.doFilter(request, response);
        } finally {
            restoreMdc(previousMdc);
        }
    }

    @Override
    protected boolean shouldNotFilterAsyncDispatch() {
        return false;
    }

    @Override
    protected boolean shouldNotFilterErrorDispatch() {
        return false;
    }

    @Override
    public int getOrder() {
        return Ordered.HIGHEST_PRECEDENCE + 1;
    }

    private static String header(HttpServletRequest request, List<String> names) {
        for (String name : names) {
            if (name == null || name.isBlank()) {
                continue;
            }
            String value = request.getHeader(name);
            if (value != null) {
                return value;
            }
        }
        return null;
    }

    private static void put(String key, String value, int maxLength) {
        if (value == null || value.isBlank()) {
            return;
        }
        String safeValue = value.replaceAll("\\p{Cntrl}", " ");
        MDC.put(key, safeValue.substring(0, Math.min(safeValue.length(), maxLength)));
    }

    private static void clearRequestKeys() {
        REQUEST_MDC_KEYS.forEach(MDC::remove);
    }

    private static void restoreMdc(Map<String, String> values) {
        if (values == null || values.isEmpty()) {
            MDC.clear();
        } else {
            MDC.setContextMap(values);
        }
    }
}
