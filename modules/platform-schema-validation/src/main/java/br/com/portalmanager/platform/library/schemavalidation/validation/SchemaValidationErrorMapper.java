package br.com.portalmanager.platform.library.schemavalidation.validation;

import br.com.portalmanager.platform.library.schemavalidation.message.SchemaValidationMessageKeys;
import com.networknt.schema.Error;

import java.util.LinkedHashMap;
import java.util.Map;

final class SchemaValidationErrorMapper {

    private static final int FIELD_ONLY = -1;

    private static final Map<String, Integer> KEYWORD_ARGUMENT_INDEX = Map.ofEntries(
            Map.entry("type", 1),
            Map.entry("enum", 0),
            Map.entry("const", 0),
            Map.entry("format", 0),
            Map.entry("dependentRequired", 1),
            Map.entry("minLength", 0),
            Map.entry("maxLength", 0),
            Map.entry("minimum", 0),
            Map.entry("maximum", 0),
            Map.entry("exclusiveMinimum", 0),
            Map.entry("exclusiveMaximum", 0),
            Map.entry("multipleOf", 0),
            Map.entry("minItems", 0),
            Map.entry("maxItems", 0),
            Map.entry("minContains", 0),
            Map.entry("maxContains", 0),
            Map.entry("minProperties", 0),
            Map.entry("maxProperties", 0),
            Map.entry("required", FIELD_ONLY),
            Map.entry("pattern", FIELD_ONLY),
            Map.entry("additionalProperties", FIELD_ONLY),
            Map.entry("uniqueItems", FIELD_ONLY),
            Map.entry("propertyNames", FIELD_ONLY),
            Map.entry("oneOf", FIELD_ONLY),
            Map.entry("not", FIELD_ONLY),
            Map.entry("unevaluatedProperties", FIELD_ONLY),
            Map.entry("unevaluatedItems", FIELD_ONLY)
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

        if (keyword == null || keyword.isBlank()) {
            return fieldOnly(field);
        }

        Integer argumentIndex = KEYWORD_ARGUMENT_INDEX.get(keyword);
        if (argumentIndex == null) {
            return rawParameters(field, error.getArguments());
        }
        if (argumentIndex == FIELD_ONLY) {
            return fieldOnly(field);
        }

        return withValue(field, argument(error, argumentIndex));
    }

    private Map<String, Object> fieldOnly(String field) {
        return Map.of("0", field);
    }

    private Map<String, Object> withValue(String field, String value) {
        if (value == null) {
            return fieldOnly(field);
        }
        return Map.of("0", field, "1", value);
    }

    private Map<String, Object> rawParameters(String field, Object[] arguments) {
        if (arguments == null || arguments.length == 0) {
            return fieldOnly(field);
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
        if (arguments == null || index < 0 || index >= arguments.length || arguments[index] == null) {
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
