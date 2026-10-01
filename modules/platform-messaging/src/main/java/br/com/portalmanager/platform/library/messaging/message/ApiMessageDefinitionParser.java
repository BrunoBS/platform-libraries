package br.com.portalmanager.platform.library.messaging.message;

import br.com.portalmanager.platform.library.messaging.exception.PlatformConfigurationException;
import br.com.portalmanager.platform.library.messaging.model.ApiMessage;

import java.util.Locale;

final class ApiMessageDefinitionParser {

    private static final String DELIMITER_REGEX = "\\|";
    private static final int EXPECTED_FIELDS = 4;

    ApiMessage parse(String key, Locale locale, String definition) {
        String[] fields = definition.split(DELIMITER_REGEX, -1);
        if (fields.length != EXPECTED_FIELDS) {
            throw new PlatformConfigurationException(
                    PlatformTechnicalErrors.invalidMessageDefinition(
                            key,
                            "expected format code|httpStatus|message|solution"
                    )
            );
        }

        String code = requireValue(fields[0], key, "code");
        int httpStatus = parseHttpStatus(fields[1], key);
        String message = requireValue(fields[2], key, "message");
        String solution = fields[3].isBlank() ? null : fields[3].trim();

        return new ApiMessage(
                code,
                key,
                locale.toLanguageTag(),
                message,
                solution,
                httpStatus
        );
    }

    private String requireValue(String value, String key, String field) {
        if (value == null || value.isBlank()) {
            throw new PlatformConfigurationException(
                    PlatformTechnicalErrors.invalidMessageDefinition(
                            key,
                            field + " is required"
                    )
            );
        }
        return value.trim();
    }

    private int parseHttpStatus(String value, String key) {
        try {
            int httpStatus = Integer.parseInt(requireValue(value, key, "httpStatus"));
            if (httpStatus < 100 || httpStatus > 599) {
                throw new PlatformConfigurationException(
                        PlatformTechnicalErrors.invalidMessageDefinition(
                                key,
                                "httpStatus must be between 100 and 599"
                        )
                );
            }
            return httpStatus;
        } catch (NumberFormatException exception) {
            throw new PlatformConfigurationException(
                    PlatformTechnicalErrors.invalidMessageDefinition(
                            key,
                            "httpStatus must be numeric"
                    ),
                    exception
            );
        }
    }
}
