package br.com.portalmanager.platform.library.observability.logging.sanitizer;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

public final class LogSanitizers {

    private static final List<LogSanitizer> CUSTOM = new CopyOnWriteArrayList<>();

    private LogSanitizers() {}

    public static String sanitize(String value) {
        String sanitized = DefaultLogSanitizer.INSTANCE.sanitize(value);
        for (LogSanitizer sanitizer : CUSTOM) {
            sanitized = sanitizer.sanitize(sanitized);
        }
        return sanitized;
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
