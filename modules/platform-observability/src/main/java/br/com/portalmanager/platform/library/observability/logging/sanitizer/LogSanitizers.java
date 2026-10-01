package br.com.portalmanager.platform.library.observability.logging.sanitizer;

import java.util.Collection;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

public final class LogSanitizers {

    private static final List<LogSanitizer> CUSTOM = new CopyOnWriteArrayList<>();
    private static volatile LogSanitizer configuredFieldSanitizer = value -> value;

    private LogSanitizers() {}

    public static String sanitize(String value) {
        String sanitized = DefaultLogSanitizer.INSTANCE.sanitize(value);
        sanitized = configuredFieldSanitizer.sanitize(sanitized);
        for (LogSanitizer sanitizer : CUSTOM) {
            sanitized = sanitizer.sanitize(sanitized);
        }
        return sanitized;
    }

    public static void configureAdditionalSensitiveFields(Collection<String> fields) {
        configuredFieldSanitizer = SensitiveFieldLogSanitizer.of(fields);
    }

    public static void register(LogSanitizer sanitizer) {
        if (sanitizer != null) {
            CUSTOM.add(sanitizer);
        }
    }

    public static void unregister(LogSanitizer sanitizer) {
        CUSTOM.remove(sanitizer);
    }
}
