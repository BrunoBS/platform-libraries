package br.com.portalmanager.platform.library.schemavalidation.validation;

import br.com.portalmanager.platform.library.schemavalidation.message.SchemaValidationMessageKeys;
import com.networknt.schema.Error;

final class SchemaValidationErrorMapper {

    MappedValidationError map(Error error, String field) {
        return new MappedValidationError(field, messageKey(error));
    }

    private String messageKey(Error error) {
        if (error == null || error.getKeyword() == null) {
            return SchemaValidationMessageKeys.INVALID;
        }

        return switch (error.getKeyword()) {
            case "required" -> SchemaValidationMessageKeys.REQUIRED;
            case "type" -> SchemaValidationMessageKeys.TYPE;
            case "minLength" -> SchemaValidationMessageKeys.MIN_LENGTH;
            case "maxLength" -> SchemaValidationMessageKeys.MAX_LENGTH;
            case "minimum" -> SchemaValidationMessageKeys.MINIMUM;
            case "maximum" -> SchemaValidationMessageKeys.MAXIMUM;
            case "pattern" -> SchemaValidationMessageKeys.PATTERN;
            case "enum" -> SchemaValidationMessageKeys.ENUM;
            case "additionalProperties" -> SchemaValidationMessageKeys.ADDITIONAL_PROPERTIES;
            default -> SchemaValidationMessageKeys.INVALID;
        };
    }

    record MappedValidationError(String field, String messageKey) {
    }
}
