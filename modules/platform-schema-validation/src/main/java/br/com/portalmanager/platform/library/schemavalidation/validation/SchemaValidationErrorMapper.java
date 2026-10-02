package br.com.portalmanager.platform.library.schemavalidation.validation;

import br.com.portalmanager.platform.library.schemavalidation.message.SchemaValidationMessageKeys;
import com.networknt.schema.Error;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

final class SchemaValidationErrorMapper {

    private static final Map<String, Integer> VALUE_ARGUMENT_INDEX = Map.of(
            "type", 1,
            "enum", 0,
            "const", 0,
            "format", 0,
            "dependentRequired", 1,
            "minLength", 0,
            "maxLength", 0,
            "minimum", 0,
            "maximum", 0,
            "exclusiveMinimum", 0,
            "exclusiveMaximum", 0,
            "multipleOf", 0,
            "minItems", 0,
            "maxItems", 0,
            "minContains", 0,
            "maxContains", 0,
            "minProperties", 0,
            "maxProperties", 0
    );

    private static final Set<String> FIELD_ONLY_KEYWORDS = Set.of(
            "required", "pattern", "additionalProperties", "uniqueItems",
            "propertyNames", "oneOf", "not",
            "unevaluatedProperties", "unevaluatedItems"
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

        if (keyword == null || keyword.isBlank() || FIELD_ONLY_KEYWORDS.contains(keyword)) {
            return Map.of("0", field);
        }

        Integer argumentIndex = VALUE_ARGUMENT_INDEX.get(keyword);
        if (argumentIndex != null) {
            return withValue(field, argument(error, argumentIndex));
        }

        return rawParameters(field, error.getArguments());
    }

    private Map<String, Object> withValue(String field, String value) {
        if (value == null) {
            return Map.of("0", field);
        }
        return Map.of("0", field, "1", value);
    }

    private Map<String, Object> rawParameters(String field, Object[] arguments) {
        if (arguments == null || arguments.length == 0) {
            return Map.of("0", field);
        }

        Map<String, Object> parameters = new LinkedHashMap<>();
        parameters.put("0", field);
        for (int index = 0; index < arguments.length; index++) {
            if (arguments[index] != null) {
                parameters.put(String.valueOf(index + 1), String.valueOf(arguments[index]));
            }
        }
        return Map.copyOf(parameters);
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
