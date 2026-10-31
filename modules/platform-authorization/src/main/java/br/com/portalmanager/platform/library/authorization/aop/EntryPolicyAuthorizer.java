package br.com.portalmanager.platform.library.authorization.aop;

import br.com.portalmanager.platform.library.authorization.annotation.AuthorizationRequired;
import br.com.portalmanager.platform.library.authorization.config.PlatformAuthorizationProperties;
import br.com.portalmanager.platform.library.authorization.exception.ForbiddenAccessException;
import br.com.portalmanager.platform.library.authorization.message.AuthorizationMessageKeys;
import br.com.portalmanager.platform.library.authorization.model.*;
import java.util.List;
import java.util.Objects;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.aop.support.AopUtils;
import org.springframework.web.context.request.RequestContextHolder;

/** Both REAL and MOCK enforce exactly the same explicit POLICY allowlist and transport proof. */
public final class EntryPolicyAuthorizer implements org.springframework.beans.factory.SmartInitializingSingleton {
    private final PlatformAuthorizationProperties properties;
    private final List<AuthorizationEntryOriginResolver> origins;
    public EntryPolicyAuthorizer(PlatformAuthorizationProperties properties, List<AuthorizationEntryOriginResolver> origins) {
        this.properties = properties; this.origins = List.copyOf(origins);
    }

    @Override public void afterSingletonsInstantiated() {
        if (!properties.getEntryPolicies().isEmpty() && origins.size() != 1) {
            throw new br.com.portalmanager.platform.library.messaging.exception.PlatformConfigurationException(
                br.com.portalmanager.platform.library.authorization.message.AuthorizationTechnicalErrors.ENTRY_POLICY_INVALID);
        }
    }

    public Object authorize(ProceedingJoinPoint invocation, AuthorizationRequired required) throws Throwable {
        // An HTTP/MCP request cannot borrow an internal policy even when invoked on a consumer thread.
        if (RequestContextHolder.getRequestAttributes() != null || origins.size() != 1) throw deny();
        AuthorizationEntryOrigin origin = origins.getFirst().current().orElseThrow(this::deny);
        AuthorizationEntryPolicy policy = properties.getEntryPolicies().get(required.policy());
        if (policy == null || !properties.getTechnicalPrincipals().contains(policy.principal())
                || !Objects.equals(policy.entryPoint(), origin.entryPoint())
                || !Objects.equals(policy.destination(), origin.destination())
                || !Objects.equals(policy.queueUrl(), origin.queueUrl())
                || policy.action() != required.action() || policy.level() != required.level()
                || origin.messageId() == null || origin.messageId().isBlank() || origin.timestamp() == null) throw deny();
        java.lang.reflect.Method method = AopUtils.getMostSpecificMethod(((MethodSignature) invocation.getSignature()).getMethod(),
                AopUtils.getTargetClass(invocation.getTarget()));
        if (!policy.facade().equals(method.getDeclaringClass().getName() + "#" + method.getName())) throw deny();
        PolicyAuthorizationContext context = null;
        for (Object argument : invocation.getArgs()) {
            if (argument instanceof PolicyAuthorizationContext candidate) {
                if (context != null) throw deny();
                context = candidate;
            }
        }
        if (context == null || !safeCorrelation(context.correlationId())
                || !context.correlationId().equals(origin.correlationId()) || context.accountIdentifier() == null
                || !policy.scopes().contains(new AuthorizationEntryPolicy.Scope(context.accountIdentifier(),
                    context.applicationIdentifier(), context.environmentIdentifier()))) throw deny();
        UserSession session = new UserSession();
        session.setUserName(policy.principal());
        session.setAccountId(context.accountIdentifier());
        session.setApplicationId(context.applicationIdentifier());
        session.setEnvironmentId(context.environmentIdentifier());
        session.setTraceId(context.correlationId());
        // No OWNER/group privileges and no synthetic bearer token are assigned to the technical principal.
        AuthorizationContext loggingContext = new AuthorizationContext(context.correlationId(), null, null,
                context.environmentIdentifier(), context.applicationIdentifier());
        try (AuthorizationRequestContext.Scope scope = AuthorizationRequestContext.open(session, loggingContext)) {
            return invocation.proceed();
        }
    }
    private boolean safeCorrelation(String value) {
        return value != null && !value.isBlank() && value.length() <= 100
                && value.chars().noneMatch(Character::isISOControl);
    }
    private ForbiddenAccessException deny() { return new ForbiddenAccessException(AuthorizationMessageKeys.POLICY_DENIED); }
}
