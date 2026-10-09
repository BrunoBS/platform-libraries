package br.com.portalmanager.platform.library.authorization.aop;

import br.com.portalmanager.platform.library.authorization.annotation.AuthorizationRequired;
import br.com.portalmanager.platform.library.authorization.exception.UnauthorizedAccessException;
import br.com.portalmanager.platform.library.authorization.message.AuthorizationMessageKeys;
import br.com.portalmanager.platform.library.authorization.model.AuthorizationContext;
import br.com.portalmanager.platform.library.authorization.model.AuthorizationRequest;
import br.com.portalmanager.platform.library.authorization.model.AuthorizationRequestContext;
import br.com.portalmanager.platform.library.authorization.model.UserSession;
import br.com.portalmanager.platform.library.authorization.web.AuthorizationClientService;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;

@Aspect
public class AuthorizationFacadeAspect {
    private final AuthorizationClientService client;

    public AuthorizationFacadeAspect(AuthorizationClientService client) {
        this.client = client;
    }

    @Around("@annotation(required)")
    public Object authorize(ProceedingJoinPoint joinPoint, AuthorizationRequired required) throws Throwable {
        AuthorizationContext context = null;
        for (Object argument : joinPoint.getArgs()) {
            if (argument instanceof AuthorizationContext candidate) {
                context = candidate;
                break;
            }
        }
        if (context == null || context.correlationId() == null || context.correlationId().isBlank()) {
            throw new UnauthorizedAccessException(AuthorizationMessageKeys.CORRELATION_ID_MISSING);
        }
        if (context.authorization() == null || !context.authorization().startsWith("Bearer ")) {
            throw new UnauthorizedAccessException(AuthorizationMessageKeys.TOKEN_MISSING);
        }

        UserSession session = client.authorize(new AuthorizationRequest(
                context.correlationId(), context.authorization(), context.workspaceIdentifier(),
                context.environmentIdentifier(), context.applicationIdentifier(),
                required.action(), required.level()));
        if (session == null) {
            throw new UnauthorizedAccessException(AuthorizationMessageKeys.SESSION_NOT_FOUND);
        }
        try (AuthorizationRequestContext.Scope scope = AuthorizationRequestContext.open(session, context)) {
            return joinPoint.proceed();
        }
    }
}
