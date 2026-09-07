package com.empresa.platform.authorization.web;

import com.empresa.platform.authorization.model.AuthorizationPolicy;
import com.empresa.platform.authorization.model.UserContext;
import com.empresa.platform.authorization.model.UserSession;
import com.empresa.platform.authorization.registry.AuthorizationMetadataRegistry;
import com.empresa.platform.authorization.service.AuthorizationClientService;
import com.empresa.platform.messaging.config.PlatformMessagingProperties;
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

    private final AuthorizationClientService authorizationClientService;
    private final AuthorizationMetadataRegistry authorizationMetadataRegistry;
    private final PlatformMessagingProperties platformMessagingProperties;

    public AuthorizationInterceptor(
            AuthorizationClientService authorizationClientService,
            AuthorizationMetadataRegistry authorizationMetadataRegistry,
            PlatformMessagingProperties platformMessagingProperties
    ) {
        this.authorizationClientService = authorizationClientService;
        this.authorizationMetadataRegistry = authorizationMetadataRegistry;
        this.platformMessagingProperties = platformMessagingProperties;
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

        Map<String, String> pathVariables = (Map<String, String>) request.getAttribute(HandlerMapping.URI_TEMPLATE_VARIABLES_ATTRIBUTE);

        // Ajustado para ler usando a chave dinâmica configurada (ex: correlationId ou X-Correlation-Id)
        String correlationId = request.getHeader("correlationId"); // Fallback amigável
        String userAgent = request.getHeader("User-Agent");
        String authHeader = request.getHeader("Authorization");
        String accountId = null;
        String environmentId = null;
        String applicationId = null;

        // VALIDAÇÕES: Disparam erros controlados do platform-messaging que acionam o catálogo automático
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

        // Chamada oficial ao servidor central de autorização
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

        // Popula os metadados do MDC para auditoria e logs estruturados
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

    @Override
    public void afterCompletion(
            @Nonnull HttpServletRequest request,
            @Nonnull HttpServletResponse response,
            @Nonnull Object handler,
            Exception ex
    ) {
        // SEGURANÇA MÁXIMA: Limpa o ThreadLocal e o MDC para evitar Memory Leaks e mistura de sessões de usuários
        UserContext.clear();
        MDC.clear();
    }
}
