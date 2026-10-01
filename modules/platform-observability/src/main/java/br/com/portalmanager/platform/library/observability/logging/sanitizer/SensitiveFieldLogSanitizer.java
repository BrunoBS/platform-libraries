package br.com.portalmanager.platform.library.observability.logging.sanitizer;

import java.util.Collection;
import java.util.Objects;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

final class SensitiveFieldLogSanitizer implements LogSanitizer {

    private static final LogSanitizer NO_OP = value -> value;

    private final Pattern sensitiveJsonField;

    private SensitiveFieldLogSanitizer(Pattern sensitiveJsonField) {
        this.sensitiveJsonField = sensitiveJsonField;
    }

    static LogSanitizer of(Collection<String> fields) {
        if (fields == null || fields.isEmpty()) {
            return NO_OP;
        }

        String alternatives = fields.stream()
                .filter(Objects::nonNull)
                .map(String::trim)
                .filter(field -> !field.isBlank())
                .map(Pattern::quote)
                .collect(Collectors.joining("|"));

        if (alternatives.isBlank()) {
            return NO_OP;
        }

        Pattern pattern = Pattern.compile(
                "(?i)(\\\"(?:" + alternatives + ")\\\"\\s*:\\s*\\\")(.*?)(\\\")"
        );
        return new SensitiveFieldLogSanitizer(pattern);
    }

    @Override
    public String sanitize(String value) {
        if (value == null || value.isBlank()) {
            return value;
        }
        return sensitiveJsonField.matcher(value).replaceAll("$1***$3");
    }
}
