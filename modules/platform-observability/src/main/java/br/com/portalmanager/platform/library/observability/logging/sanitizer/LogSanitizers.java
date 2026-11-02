package br.com.portalmanager.platform.library.observability.logging.sanitizer;

import java.util.Collection;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

public final class LogSanitizers {

    private static final List<LogSanitizer> CUSTOM = new CopyOnWriteArrayList<>();
    private static volatile DefaultLogSanitizer builtIn = DefaultLogSanitizer.INSTANCE;

    private LogSanitizers() {}

    public static String sanitize(String value) {
        return applyCustomSanitizers(builtIn.sanitize(value));
    }

    public static void configureAdditionalSensitiveFields(Collection<String> fields) {
        builtIn = DefaultLogSanitizer.withAdditionalSensitiveFields(fields);
    }

    public static synchronized void register(LogSanitizer sanitizer) {
        if (sanitizer != null) {
            boolean alreadyRegistered = CUSTOM.stream().anyMatch(existing -> existing == sanitizer);
            if (!alreadyRegistered) {
                CUSTOM.add(sanitizer);
            }
        }
    }

    public static synchronized void unregister(LogSanitizer sanitizer) {
        CUSTOM.removeIf(existing -> existing == sanitizer);
    }

    private static String applyCustomSanitizers(String value) {
        if (value == null || value.isBlank()) {
            return value;
        }
        String sanitized = value;
        for (LogSanitizer sanitizer : CUSTOM) {
            sanitized = sanitizer.sanitize(sanitized);
        }
        return sanitized;
    }
}
