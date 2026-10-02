package br.com.portalmanager.platform.library.schemavalidation.validation;

import br.com.portalmanager.platform.library.schemavalidation.message.SchemaValidationMessageKeys;
import com.networknt.schema.Error;

import java.util.Map;
import java.util.Set;

final class SchemaValidationErrorMapper {

    private static final Set<String> LIMIT_KEYWORDS = Set.of(
            "minLength", "maxLength", "minimum", "maximum",
            "exclusiveMinimum", "exclusiveMaximum", "multipleOf",
            "minItems", "maxItems", "minContains", "maxContains",
            "minProperties", "maxProperties"
    );

    MappedValidationError map(Error error, String field) {
        String keyword = error.getKeyword();
        String mappedField = mappedField(error, field);

        return new MappedValidationError(
                mappedField,
                SchemaValidationMessageKeys.fromKeyword(keyword),
                parameters(error, mappedField)
        );
    }

    private String mappedField(Error error, String field) {
        if ("dependentRequired".equals(error.getKeyword())) {
            String requiredField = argument(error, 0);
            if (requiredField != null) {
                return requiredField;
            }
        }
        return field;
    }

    private Map<String, Object> parameters(Error error, String field) {
        String keyword = error.getKeyword();

        if ("type".equals(keyword)) {
            return withValue(field, argument(error, 1));
        }
        if ("enum".equals(keyword) || "const".equals(keyword)) {
            return withValue(field, argument(error, 0));
        }
        if ("dependentRequired".equals(keyword)) {
            return withValue(field, argument(error, 1));
        }
        if (LIMIT_KEYWORDS.contains(keyword)) {
            return withValue(field, argument(error, 0));
        }

        return Map.of("0", field);
    }

    private Map<String, Object> withValue(String field, String value) {
        if (value == null) {
            return Map.of("0", field);
        }
        return Map.of("0", field, "1", value);
    }

    private String argument(Error error, int index) {
        Object[] arguments = error.getArguments();
        if (arguments == null || index >= arguments.length || arguments[index] == null) {
            return null;
        }
        return String.valueOf(arguments[index]);
    }

    record MappedValidationError(
            String field,
            String messageKey,
            Map<String, Object> parameters
    ) {
    }
}
