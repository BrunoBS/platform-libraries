package br.com.portalmanager.platform.library.authorization.web;

import br.com.portalmanager.platform.library.authorization.model.AuthorizationContext;
import br.com.portalmanager.platform.library.authorization.config.PlatformAuthorizationProperties;
import java.util.Objects;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.core.MethodParameter;
import org.springframework.web.bind.support.WebDataBinderFactory;
import org.springframework.web.context.request.NativeWebRequest;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.web.method.support.ModelAndViewContainer;

/** Resolves HTTP entrypoint metadata without performing authorization. */
public final class AuthorizationContextResolver implements HandlerMethodArgumentResolver {

    private final PlatformAuthorizationProperties.Headers headers;

    public AuthorizationContextResolver(PlatformAuthorizationProperties properties) {
        this.headers = Objects.requireNonNull(properties, "properties").getHeaders();
    }

    @Override
    public boolean supportsParameter(MethodParameter parameter) {
        return parameter.getParameterType() == AuthorizationContext.class;
    }

    @Override
    public Object resolveArgument(MethodParameter parameter, ModelAndViewContainer mavContainer,
                                  NativeWebRequest webRequest, WebDataBinderFactory binderFactory) {
        HttpServletRequest request = webRequest.getNativeRequest(HttpServletRequest.class);
        if (request == null) {
            throw new IllegalStateException("AuthorizationContext requires an HTTP servlet request");
        }
        return new AuthorizationContext(
                request.getHeader(headers.getCorrelationId()),
                request.getHeader(headers.getAuthorization()),
                request.getHeader(headers.getWorkspaceIdentifier()),
                request.getHeader(headers.getEnvironmentIdentifier()),
                request.getHeader(headers.getApplicationIdentifier()),
                request.getRemoteAddr(),
                request.getHeader("user-agent"),
                request.getRequestURI());
    }

}
