package br.com.portalmanager.platform.library.authorization.aspect;

import br.com.portalmanager.platform.library.authorization.annotation.ResourceVisibility;
import br.com.portalmanager.platform.library.authorization.exception.UnauthorizedAccessException;
import br.com.portalmanager.platform.library.authorization.message.AuthorizationMessageKeys;
import br.com.portalmanager.platform.library.authorization.model.ParsedGroup;
import br.com.portalmanager.platform.library.authorization.model.UserContext;
import br.com.portalmanager.platform.library.authorization.model.UserSession;
import br.com.portalmanager.platform.library.authorization.resource.ResourceVisibilityFilterManager;
import br.com.portalmanager.platform.library.authorization.resource.ResourceVisibilityIdResolver;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Set;

@Aspect
public class ResourceVisibilityAspect {

    private static final Logger log = LoggerFactory.getLogger(ResourceVisibilityAspect.class);

    private final ResourceVisibilityFilterManager filterManager;
    private final ResourceVisibilityIdResolver idResolver;

    public ResourceVisibilityAspect(
            ResourceVisibilityFilterManager filterManager,
            ResourceVisibilityIdResolver idResolver
    ) {
        this.filterManager = filterManager;
        this.idResolver = idResolver;
    }

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
            log.debug("Usuário OWNER ignorou filtro Hibernate de visibilidade de recurso.");
            return joinPoint.proceed();
        }

        if (session.getGroups().isEmpty()) {
            log.warn("Tentativa de avaliar visibilidade de recurso sem grupos mapeados.");
            throw new UnauthorizedAccessException(
                    AuthorizationMessageKeys.GROUPS_NOT_FOUND
            );
        }

        Set<String> authorizerGroups = session.getAuthorizerGroups().stream()
                .map(ParsedGroup::authorizer)
                .filter(value -> value != null && !value.isBlank())
                .collect(java.util.stream.Collectors.toUnmodifiableSet());

        Set<Long> authorizedIds = idResolver.findAuthorizedIds(
                resourceVisibility,
                authorizerGroups
        );

        boolean enabled = filterManager.enable(authorizedIds);
        try {
            return joinPoint.proceed();
        } finally {
            if (enabled) {
                filterManager.disable();
            }
        }
    }
}
