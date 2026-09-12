package com.empresa.platform.authorization.web;

import com.empresa.platform.authorization.model.AuthorizationPolicy;
import com.empresa.platform.authorization.model.UserContext;
import com.empresa.platform.authorization.model.UserSession;
import com.empresa.platform.authorization.registry.AuthorizationMetadataRegistry;
import com.empresa.platform.authorization.service.AuthorizationClientService;
import com.empresa.platform.messaging.exception.UnauthorizedException;
import jakarta.annotation.Nonnull;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.MDC;
import org.springframework.stereotype.Component;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.HandlerInterceptor;
import org.springframework.web.servlet.HandlerMapping;

import java.util.Map;

@Component
@SuppressWarnings("unchecked")
public class AuthorizationInterceptor implements HandlerInterceptor {

    public static final String CORRELATION_ID_HEADER = "X-Correlation-Id";
    public static final String LEGACY_CORRELATION_ID_HEADER = "correlationId";

    private final AuthorizationClientService authorizationClientService;
    private final AuthorizationMetadataRegistry authorizationMetadataRegistry;

    public AuthorizationInterceptor(
            AuthorizationClientService authorizationClientService,
            AuthorizationMetadataRegistry authorizationMetadataRegistry
    ) {
        this.authorizationClientService = authorizationClientService;
        this.authorizationMetadataRegistry = authorizationMetadataRegistry;
    }

    @Override
    public boolean preHandle(
            @Nonnull HttpServletRequest request,
            @Nonnull HttpServletResponse response,
            @Nonnull Object handler
    ) throws Exception {

        if (!(handler instanceof HandlerMethod handlerMethod)) {
            return true;
        }

        AuthorizationPolicy policy = authorizationMetadataRegistry.resolve(
                handlerMethod.getBeanType(),
                handlerMethod.getMethod()
        );

        Map<String, String> pathVariables = (Map<String, String>) request.getAttribute(
                HandlerMapping.URI_TEMPLATE_VARIABLES_ATTRIBUTE
        );

        String correlationId = resolveCorrelationId(request);
        String userAgent = request.getHeader("User-Agent");
        String authHeader = request.getHeader("Authorization");
        String accountId = null;
        String environmentId = null;
        String applicationId = null;

        if (correlationId == null || correlationId.isBlank()) {
            throw new UnauthorizedException("CORRELATION_ID_MISSING");
        }
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            throw new UnauthorizedException("AUTHORIZATION_TOKEN_MISSING");
        }

        if (pathVariables != null) {
            accountId = pathVariables.get("accountId");
            environmentId = pathVariables.get("environmentId");
            applicationId = pathVariables.get("applicationId");
        }

        UserSession body = authorizationClientService.authorize(
                correlationId,
                authHeader,
                accountId,
                environmentId,
                applicationId,
                request.getMethod(),
                policy.level()
        );

        UserContext.set(body);

        MDC.put("correlationId", body.getTraceId());
        MDC.put("username", body.getUserName());
        MDC.put("clientIp", request.getRemoteAddr());
        MDC.put("userAgent", userAgent != null ? userAgent : "unknown");
        MDC.put("uri", request.getRequestURI());
        MDC.put("accountId", body.getAccountId());
        MDC.put("environmentId", body.getEnvironmentId());
        MDC.put("applicationId", body.getApplicationId());

        return true;
    }

    private String resolveCorrelationId(HttpServletRequest request) {
        String correlationId = request.getHeader(CORRELATION_ID_HEADER);
        if (correlationId == null || correlationId.isBlank()) {
            correlationId = request.getHeader(LEGACY_CORRELATION_ID_HEADER);
        }
        return correlationId;
    }

    @Override
    public void afterCompletion(
            @Nonnull HttpServletRequest request,
            @Nonnull HttpServletResponse response,
            @Nonnull Object handler,
            Exception ex
    ) {
        UserContext.clear();
        MDC.clear();
    }
}
