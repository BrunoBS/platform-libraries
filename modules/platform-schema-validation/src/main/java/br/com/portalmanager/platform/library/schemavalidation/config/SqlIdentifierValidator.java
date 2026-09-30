package br.com.portalmanager.platform.library.schemavalidation.config;

import java.util.regex.Pattern;

public final class SqlIdentifierValidator {

    private static final Pattern STRICT_SQL_PATTERN = Pattern.compile("^[a-zA-Z0-9_]+$");

    private SqlIdentifierValidator() {
    }

    public static String validate(String value) {
        if (value == null || value.isBlank()) {
            throw new IllegalStateException("Schema validation view name is required");
        }
        if (!STRICT_SQL_PATTERN.matcher(value).matches()) {
            throw new IllegalStateException("Invalid schema validation SQL identifier: " + value);
        }
        return value;
    }
}
