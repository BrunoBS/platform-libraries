package br.com.portalmanager.platform.library.authorization.model;

import org.slf4j.MDC;
import org.springframework.core.task.TaskDecorator;

import java.util.Map;
import java.util.Objects;
import java.util.concurrent.Callable;

/**
 * Explicit context propagation across executor boundaries, including virtual threads.
 * Apply as a TaskDecorator to Spring executors, or wrap individual tasks.
 * Never use InheritableThreadLocal: authorization must not be inherited implicitly.
 */
public final class AuthorizationContextPropagation {
    private AuthorizationContextPropagation() {}

    public static TaskDecorator taskDecorator() {
        return AuthorizationContextPropagation::wrap;
    }

    public static Runnable wrap(Runnable task) {
        Objects.requireNonNull(task, "task");
        UserSession capturedSession = UserContext.get().orElse(null);
        Map<String, String> capturedMdc = MDC.getCopyOfContextMap();
        return () -> {
            UserSession previous = UserContext.get().orElse(null);
            Map<String, String> previousMdc = MDC.getCopyOfContextMap();
            try {
                UserContext.set(capturedSession);
                restoreMdc(capturedMdc);
                task.run();
            } finally {
                UserContext.set(previous);
                restoreMdc(previousMdc);
            }
        };
    }

    public static <T> Callable<T> wrap(Callable<T> task) {
        Objects.requireNonNull(task, "task");
        UserSession capturedSession = UserContext.get().orElse(null);
        Map<String, String> capturedMdc = MDC.getCopyOfContextMap();
        return () -> {
            UserSession previous = UserContext.get().orElse(null);
            Map<String, String> previousMdc = MDC.getCopyOfContextMap();
            try {
                UserContext.set(capturedSession);
                restoreMdc(capturedMdc);
                return task.call();
            } finally {
                UserContext.set(previous);
                restoreMdc(previousMdc);
            }
        };
    }

    private static void restoreMdc(Map<String, String> values) {
        if (values == null) MDC.clear();
        else MDC.setContextMap(values);
    }
}
