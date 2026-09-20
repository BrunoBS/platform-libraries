package br.com.portalmanager.core.messaging.config;

import java.util.regex.Pattern;

public final class SqlIdentifierValidator {

    private static final Pattern STRICT_SQL_PATTERN = Pattern.compile("^[a-zA-Z0-9_]+$");

    private SqlIdentifierValidator() {
        // Construtor privado para evitar instanciação de classe utilitária
    }

    public static String validate(String value) {
        if (value == null || !STRICT_SQL_PATTERN.matcher(value).matches()) {
            throw new IllegalArgumentException("Invalid SQL identifier detected: " + value);
        }
        return value;
    }
}
