package br.com.portalmanager.platform.library.authorization.web;

import br.com.portalmanager.platform.library.authorization.model.AuthorizationContext;
import br.com.portalmanager.platform.library.authorization.config.PlatformAuthorizationProperties;
import java.util.Objects;
import java.util.List;
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
                header(request, headers.getCorrelationId()),
                header(request, headers.getAuthorization()),
                header(request, headers.getWorkspaceIdentifier()),
                header(request, headers.getEnvironmentIdentifier()),
                header(request, headers.getApplicationIdentifier()),
                request.getRemoteAddr(),
                request.getHeader("user-agent"),
                request.getRequestURI());
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
}
