package br.com.portalmanager.platform.library.redis.internal;

import br.com.portalmanager.platform.library.messaging.exception.*;
import br.com.portalmanager.platform.library.messaging.model.PlatformErrorDefinition;

import java.util.concurrent.CompletionException;
import java.util.concurrent.ExecutionException;
import java.util.function.Supplier;

/** Maps errors without retaining secrets from driver exception messages or payloads. */
public final class RedisErrors {
    private RedisErrors() {}

    public static ValidationException invalid() {
        return new ValidationException("redis.validation");
    }

    public static PlatformConfigurationException configuration() {
        return new PlatformConfigurationException(
                new PlatformErrorDefinition(
                        "PLT-REDIS-008",
                        "Invalid Platform Redis configuration",
                        "Review platform.redis and spring.data.redis properties.",
                        500));
    }

    public static ApiException error(String category, Throwable source) {
        // Deliberately do not attach the original throwable: messages can contain credentials or
        // values.
        return new ApiException(
                "redis." + category,
                source == null
                        ? null
                        : new Exception(
                                "Technical category: " + source.getClass().getSimpleName()));
    }

    public static ApiException translate(Throwable source) {
        Throwable current = source;
        while ((current instanceof CompletionException || current instanceof ExecutionException)
                && current.getCause() != null) current = current.getCause();
        if (current instanceof ApiException api) return api;
        for (Throwable cause = current; cause != null; cause = cause.getCause()) {
            String name = cause.getClass().getSimpleName();
            String message = cause.getMessage() == null ? "" : cause.getMessage();
            if (name.contains("Timeout")) return error("timeout", current);
            if (message.contains("NOPERM")
                    || message.contains("NOAUTH")
                    || message.contains("WRONGPASS")
                    || name.contains("Authentication")) return error("authentication", current);
            if (message.contains("WRONGTYPE")
                    || message.contains("not an integer")
                    || message.contains("not a valid float")
                    || message.contains("would overflow")) return error("type", current);
            if (message.contains("PLATFORM_REDIS_LIMIT")) return error("limit", current);
        }
        return error("unavailable", current);
    }

    public static <T> T call(Supplier<T> action) {
        try {
            return action.get();
        } catch (RuntimeException failure) {
            throw translate(failure);
        }
    }
}
