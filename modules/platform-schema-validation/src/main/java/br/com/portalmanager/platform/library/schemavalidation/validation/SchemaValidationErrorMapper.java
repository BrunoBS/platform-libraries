package br.com.portalmanager.platform.library.schemavalidation.validation;

import br.com.portalmanager.platform.library.schemavalidation.message.SchemaValidationMessageKeys;
import com.networknt.schema.Error;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;
import java.util.function.BiFunction;

final class SchemaValidationErrorMapper {

    private static final Set<String> LIMIT_KEYWORDS = Set.of(
            "minLength", "maxLength", "minimum", "maximum",
            "exclusiveMinimum", "exclusiveMaximum", "multipleOf",
            "minItems", "maxItems", "minContains", "maxContains",
            "minProperties", "maxProperties"
    );

    private static final Set<String> FIELD_ONLY_KEYWORDS = Set.of(
            "required", "pattern", "additionalProperties", "uniqueItems",
            "propertyNames", "oneOf", "not",
            "unevaluatedProperties", "unevaluatedItems"
    );

    private static final Map<String, BiFunction<Error, String, Map<String, Object>>> PARAMETER_MAPPERS =
            parameterMappers();

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
            return Map.of("0", field);
        }

        BiFunction<Error, String, Map<String, Object>> mapper = PARAMETER_MAPPERS.get(keyword);
        if (mapper != null) {
            return mapper.apply(error, field);
        }

        return rawParameters(field, error.getArguments());
    }

    private static Map<String, Object> withArgument(String field, int index, Error error) {
        return withValue(field, argument(error, index));
    }

    private static Map<String, Object> withValue(String field, String value) {
        if (value == null) {
            return Map.of("0", field);
        }
        return Map.of("0", field, "1", value);
    }

    private static Map<String, Object> fieldOnly(String field) {
        return Map.of("0", field);
    }

    private static Map<String, Object> rawParameters(String field, Object[] arguments) {
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

    private static String argument(Error error, int index) {
        Object[] arguments = error.getArguments();
        if (arguments == null || index >= arguments.length || arguments[index] == null) {
            return null;
        }
        return String.valueOf(arguments[index]);
    }

    private static Map<String, BiFunction<Error, String, Map<String, Object>>> parameterMappers() {
        Map<String, BiFunction<Error, String, Map<String, Object>>> mappers = new LinkedHashMap<>();

        mappers.put("type", (error, field) -> withArgument(field, 1, error));
        mappers.put("enum", (error, field) -> withArgument(field, 0, error));
        mappers.put("const", (error, field) -> withArgument(field, 0, error));
        mappers.put("format", (error, field) -> withArgument(field, 0, error));
        mappers.put("dependentRequired", (error, field) -> withArgument(field, 1, error));

        LIMIT_KEYWORDS.forEach(keyword ->
                mappers.put(keyword, (error, field) -> withArgument(field, 0, error))
        );

        FIELD_ONLY_KEYWORDS.forEach(keyword ->
                mappers.put(keyword, (error, field) -> fieldOnly(field))
        );

        return Map.copyOf(mappers);
    }

    record MappedValidationError(
            String field,
            String messageKey,
            Map<String, Object> parameters
    ) {
    }
}
