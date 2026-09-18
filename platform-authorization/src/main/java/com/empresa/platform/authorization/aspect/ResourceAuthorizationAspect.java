package com.empresa.platform.authorization.aspect;

import com.empresa.platform.authorization.annotation.ResourceAuthorization;
import com.empresa.platform.authorization.exception.ForbiddenAccessException;
import com.empresa.platform.authorization.exception.UnauthorizedAccessException;
import com.empresa.platform.authorization.message.AuthorizationMessageKeys;
import com.empresa.platform.authorization.model.UserContext;
import com.empresa.platform.authorization.model.UserSession;
import com.empresa.platform.authorization.resource.AuthorizableResource;
import com.empresa.platform.authorization.resource.ResourceAuthorizationFilterManager;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collection;

@Aspect
public class ResourceAuthorizationAspect {

    private static final Logger log = LoggerFactory.getLogger(ResourceAuthorizationAspect.class);

    private final ResourceAuthorizationFilterManager filterManager;

    public ResourceAuthorizationAspect() {
        this(null);
    }

    public ResourceAuthorizationAspect(ResourceAuthorizationFilterManager filterManager) {
        this.filterManager = filterManager;
    }

    @Around("@annotation(resourceAuthorization)")
    public Object authorize(
            ProceedingJoinPoint joinPoint,
            ResourceAuthorization resourceAuthorization
    ) throws Throwable {

        UserSession session = UserContext.get()
                .orElseThrow(() -> new UnauthorizedAccessException(
                        AuthorizationMessageKeys.SESSION_NOT_FOUND
                ));

        if (session.isOwner()) {
            log.debug("Usuário OWNER ignorou validação de autorização de recurso.");
            return joinPoint.proceed();
        }

        if (session.getGroups().isEmpty()) {
            log.warn("Tentativa de acesso a recurso sem grupos mapeados.");
            throw new UnauthorizedAccessException(
                    AuthorizationMessageKeys.GROUPS_NOT_FOUND
            );
        }

        boolean filterEnabled = false;
        Object result;

        try {
            if (filterManager != null) {
                filterEnabled = filterManager.enable(session);
            }
            result = joinPoint.proceed();
        } finally {
            if (filterEnabled) {
                filterManager.disable();
            }
        }

        // Defense in depth and fallback for non-JPA/native-query results.
        if (result instanceof Collection<?> collection) {
            return collection.stream()
                    .filter(item -> hasAccess(item, session))
                    .toList();
        }

        if (result != null && !hasAccess(result, session)) {
            log.warn(
                    "Acesso negado ao recurso. gruposUsuario={} recurso={}",
                    session.getGroups(),
                    result.getClass().getSimpleName()
            );

            throw new ForbiddenAccessException(
                    AuthorizationMessageKeys.RESOURCE_ACCESS_DENIED
            );
        }

        return result;
    }

    private boolean hasAccess(Object resource, UserSession session) {
        if (resource instanceof AuthorizableResource authorizable) {
            return session.hasAuthorizer(authorizable.getAuthorizerGroup());
        }

        return true;
    }
}
