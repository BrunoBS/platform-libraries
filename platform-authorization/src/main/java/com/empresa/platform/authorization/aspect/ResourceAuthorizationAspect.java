package com.empresa.platform.authorization.aspect;

import com.empresa.platform.authorization.annotation.ResourceAuthorization;
import com.empresa.platform.authorization.message.AuthorizationMessageKeys;
import com.empresa.platform.authorization.model.UserContext;
import com.empresa.platform.authorization.model.UserSession;
import com.empresa.platform.authorization.resource.AuthorizableResource;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import com.empresa.platform.messaging.exception.ForbiddenException;
import com.empresa.platform.messaging.exception.UnauthorizedException;

import java.util.Collection;

@Aspect
public class ResourceAuthorizationAspect {

    private static final Logger log = LoggerFactory.getLogger(ResourceAuthorizationAspect.class);

    @Around("@annotation(resourceAuthorization)")
    public Object authorize(
            ProceedingJoinPoint joinPoint,
            ResourceAuthorization resourceAuthorization
    ) throws Throwable {

        UserSession session = UserContext.get()
                .orElseThrow(() -> new UnauthorizedException(
                        AuthorizationMessageKeys.SESSION_NOT_FOUND
                ));

        if (session.isOwner()) {
            log.debug("Usuário OWNER ignorou validação de autorização de recurso.");
            return joinPoint.proceed();
        }

        if (session.getGroups().isEmpty()) {
            log.warn("Tentativa de acesso a recurso sem grupos mapeados.");
            throw new UnauthorizedException(
                    AuthorizationMessageKeys.GROUPS_NOT_FOUND
            );
        }

        Object result = joinPoint.proceed();

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

            throw new ForbiddenException(
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
