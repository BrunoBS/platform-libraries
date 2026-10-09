package br.com.portalmanager.platform.library.authorization.aop;

import br.com.portalmanager.platform.library.authorization.annotation.AuthorizationRequired;
import br.com.portalmanager.platform.library.authorization.config.PlatformAuthorizationProperties;
import br.com.portalmanager.platform.library.authorization.model.AuthorizerGroupParser;
import br.com.portalmanager.platform.library.authorization.model.AuthorizationContext;
import br.com.portalmanager.platform.library.authorization.model.ParsedGroup;
import br.com.portalmanager.platform.library.authorization.model.UserContext;
import br.com.portalmanager.platform.library.authorization.model.UserSession;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.slf4j.MDC;

import java.time.Instant;
import java.util.Set;

@Aspect
public class MockAuthorizationFacadeAspect {
    private final PlatformAuthorizationProperties properties;

    public MockAuthorizationFacadeAspect(PlatformAuthorizationProperties properties) {
        this.properties = properties;
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
        UserSession previous = UserContext.get().orElse(null);
        String correlationId = MDC.get("correlationId");
        String username = MDC.get("username");
        try {
            UserSession session = createSession(properties.getMock());
            UserContext.set(session);
            if (context != null && context.correlationId() != null) {
                MDC.put("correlationId", context.correlationId());
            }
            if (session.getUserName() != null) MDC.put("username", session.getUserName());
            return joinPoint.proceed();
        } finally {
            UserContext.set(previous);
            restore("correlationId", correlationId);
            restore("username", username);
        }
    }

    private UserSession createSession(PlatformAuthorizationProperties.Mock mock) {
        UserSession session = new UserSession();
        session.setUserName(mock.getUserName());
        session.setEmail(mock.getEmail());
        session.setAccountId(mock.getAccountId());
        session.setApplicationId(mock.getApplicationId());
        session.setEnvironmentId(mock.getEnvironmentId());
        session.setTraceId(mock.getTraceId());
        session.setExpirationTime(Instant.now().plusSeconds(3600).toEpochMilli());
        session.setGroups(mock.getGroups());
        if (!mock.getAuthorizerGroups().isEmpty()) {
            session.setAuthorizerGroups(Set.copyOf(mock.getAuthorizerGroups().stream()
                    .map(group -> new ParsedGroup(group.getFullGroup(), group.getProfile(),
                            group.getEnvironment(), group.getAuthorizer())).toList()));
        } else {
            Set<ParsedGroup> parsed = AuthorizerGroupParser.parseAll(mock.getGroups());
            session.setAuthorizerGroups(parsed.isEmpty()
                    ? Set.of(new ParsedGroup("GUEST", "GUEST", mock.getEnvironmentId(), "GUEST"))
                    : parsed);
        }
        return session;
    }

    private static void restore(String key, String value) {
        if (value == null) MDC.remove(key);
        else MDC.put(key, value);
    }
}
