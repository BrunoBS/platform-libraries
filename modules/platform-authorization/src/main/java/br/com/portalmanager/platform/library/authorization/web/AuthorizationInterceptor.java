package br.com.portalmanager.platform.library.authorization.web;

import br.com.portalmanager.platform.library.authorization.exception.UnauthorizedAccessException;
import br.com.portalmanager.platform.library.authorization.message.AuthorizationMessageKeys;
import br.com.portalmanager.platform.library.authorization.model.AuthorizationPolicy;
import br.com.portalmanager.platform.library.authorization.model.AuthorizationRequest;
import br.com.portalmanager.platform.library.authorization.model.UserSession;
import br.com.portalmanager.platform.library.authorization.registry.AuthorizationMetadataRegistry;
import br.com.portalmanager.platform.library.authorization.service.AuthorizationClientService;
import jakarta.annotation.Nonnull;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.HandlerInterceptor;
import org.springframework.web.servlet.HandlerMapping;

import java.util.Map;

@SuppressWarnings("unchecked")
public class AuthorizationInterceptor implements HandlerInterceptor {

    public static final String CORRELATION_ID_HEADER = "correlationId";

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
        String authHeader = request.getHeader("Authorization");
        String workspaceIdentifier = null;
        String environmentIdentifier = null;
        String applicationIdentifier = null;

        if (correlationId == null || correlationId.isBlank()) {
            throw new UnauthorizedAccessException(AuthorizationMessageKeys.CORRELATION_ID_MISSING);
        }
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            throw new UnauthorizedAccessException(AuthorizationMessageKeys.TOKEN_MISSING);
        }

        if (pathVariables != null) {
            workspaceIdentifier = pathVariables.get(policy.workspacePathVariable());
            environmentIdentifier = pathVariables.get(policy.environmentPathVariable());
            applicationIdentifier = pathVariables.get(policy.applicationPathVariable());
        }

        AuthorizationRequest authorizationRequest = new AuthorizationRequest(
                correlationId,
                authHeader,
                workspaceIdentifier,
                environmentIdentifier,
                applicationIdentifier,
                request.getMethod(),
                policy.level()
        );

        UserSession body = authorizationClientService.authorize(authorizationRequest);

        AuthorizationRequestContext.set(body, request, "unknown");

        return true;
    }

    private String resolveCorrelationId(HttpServletRequest request) {
        return request.getHeader(CORRELATION_ID_HEADER);
    }

    @Override
    public void afterCompletion(
            @Nonnull HttpServletRequest request,
            @Nonnull HttpServletResponse response,
            @Nonnull Object handler,
            Exception ex
    ) {
        AuthorizationRequestContext.clear();
    }
}
