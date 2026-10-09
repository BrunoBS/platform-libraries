package br.com.portalmanager.platform.library.authorization.model;

import org.slf4j.MDC;
import java.util.Map;

/** Scoped session and logging metadata, independent of the transport. */
public final class AuthorizationRequestContext {
    private AuthorizationRequestContext() {}

    public static Scope open(UserSession session, AuthorizationContext context) {
        UserSession previous = UserContext.get().orElse(null);
        Map<String, String> previousMdc = MDC.getCopyOfContextMap();
        try {
            UserContext.set(session);
            put("correlationId", context == null ? null : context.correlationId());
            put("username", session.getUserName());
            put("accountId", session.getAccountId());
            put("applicationId", session.getApplicationId());
            put("environmentId", session.getEnvironmentId());
            if (context != null) {
                put("clientIp", context.clientIp());
                put("userAgent", context.userAgent());
                put("uri", context.uri());
            }
            return () -> {
                UserContext.set(previous);
                if (previousMdc == null) MDC.clear();
                else MDC.setContextMap(previousMdc);
            };
        } catch (RuntimeException | Error failure) {
            UserContext.set(previous);
            if (previousMdc == null) MDC.clear();
            else MDC.setContextMap(previousMdc);
            throw failure;
        }
    }

    private static void put(String key, String value) {
        if (value != null) MDC.put(key, value);
    }

    @FunctionalInterface
    public interface Scope extends AutoCloseable {
        @Override void close();
    }
}
