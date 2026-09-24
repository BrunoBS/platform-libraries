package br.com.portalmanager.platform.messaging.config;

import br.com.portalmanager.platform.messaging.exception.PlatformConfigurationException;
import br.com.portalmanager.platform.messaging.message.PlatformTechnicalErrors;

import java.util.regex.Pattern;

public final class SqlIdentifierValidator {

    private static final Pattern STRICT_SQL_PATTERN = Pattern.compile("^[a-zA-Z0-9_]+$");

    private SqlIdentifierValidator() {
        // Construtor privado para evitar instanciação de classe utilitária
    }

    public static String validate(String value) {
        if (value == null || value.isBlank()) {
            throw new PlatformConfigurationException(
                    PlatformTechnicalErrors.VIEW_NAME_REQUIRED
            );
        }
        if (!STRICT_SQL_PATTERN.matcher(value).matches()) {
            throw new PlatformConfigurationException(
                    PlatformTechnicalErrors.invalidSqlIdentifier(value)
            );
        }
        return value;
    }
}
