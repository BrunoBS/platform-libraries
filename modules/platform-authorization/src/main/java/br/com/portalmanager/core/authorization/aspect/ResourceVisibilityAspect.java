package br.com.portalmanager.core.authorization.aspect;

import br.com.portalmanager.core.authorization.annotation.ResourceVisibility;
import br.com.portalmanager.core.authorization.exception.ForbiddenAccessException;
import br.com.portalmanager.core.authorization.exception.UnauthorizedAccessException;
import br.com.portalmanager.core.authorization.message.AuthorizationMessageKeys;
import br.com.portalmanager.core.authorization.model.UserContext;
import br.com.portalmanager.core.authorization.model.UserSession;
import br.com.portalmanager.core.authorization.resource.AuthorizableResource;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collection;

@Aspect
public class ResourceVisibilityAspect {

    private static final Logger log = LoggerFactory.getLogger(ResourceVisibilityAspect.class);

    @Around("@annotation(resourceVisibility)")
    public Object applyVisibility(
            ProceedingJoinPoint joinPoint,
            ResourceVisibility resourceVisibility
    ) throws Throwable {

        UserSession session = UserContext.get()
                .orElseThrow(() -> new UnauthorizedAccessException(
                        AuthorizationMessageKeys.SESSION_NOT_FOUND
                ));

        if (session.isOwner()) {
            log.debug("Usuário OWNER ignorou filtro de visibilidade de recurso.");
            return joinPoint.proceed();
        }

        if (session.getGroups().isEmpty()) {
            log.warn("Tentativa de avaliar visibilidade de recurso sem grupos mapeados.");
            throw new UnauthorizedAccessException(
                    AuthorizationMessageKeys.GROUPS_NOT_FOUND
            );
        }

        Object result = joinPoint.proceed();

        if (result instanceof Collection<?> collection) {
            return collection.stream()
                    .filter(item -> isVisible(item, session))
                    .toList();
        }

        if (result != null && !isVisible(result, session)) {
            log.warn(
                    "Recurso não visível para o usuário. gruposUsuario={} recurso={}",
                    session.getGroups(),
                    result.getClass().getSimpleName()
            );

            throw new ForbiddenAccessException(
                    AuthorizationMessageKeys.RESOURCE_ACCESS_DENIED
            );
        }

        return result;
    }

    private boolean isVisible(Object resource, UserSession session) {
        if (resource instanceof AuthorizableResource authorizable) {
            return session.hasAuthorizer(authorizable.getAuthorizerGroup());
        }

        return true;
    }
}
