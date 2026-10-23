package br.com.portalmanager.platform.library.schemavalidation.validation;

import br.com.portalmanager.platform.library.messaging.exception.PlatformConfigurationException;
import br.com.portalmanager.platform.library.messaging.exception.ValidationException;
import br.com.portalmanager.platform.library.messaging.validation.ValidationResult;
import br.com.portalmanager.platform.library.schemavalidation.cache.CompiledSchemaCache;
import br.com.portalmanager.platform.library.schemavalidation.cache.NoOpCompiledSchemaCache;
import br.com.portalmanager.platform.library.schemavalidation.message.SchemaValidationMessageKeys;
import br.com.portalmanager.platform.library.schemavalidation.message.SchemaValidationTechnicalErrors;
import br.com.portalmanager.platform.library.schemavalidation.model.ResourceSchema;

import com.networknt.schema.Error;
import com.networknt.schema.Schema;
import com.networknt.schema.SchemaException;
import com.networknt.schema.SchemaLocation;
import com.networknt.schema.SchemaRegistry;
import com.networknt.schema.SpecificationVersion;

import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

public final class JsonSchemaValidator {

    private final ObjectMapper objectMapper;
    private final SchemaRegistry schemaRegistry;
    private final CompiledSchemaCache compiledSchemaCache;
    private final SchemaValidationErrorMapper errorMapper = new SchemaValidationErrorMapper();

    public JsonSchemaValidator(ObjectMapper objectMapper) {
        this(objectMapper, new NoOpCompiledSchemaCache());
    }

    public JsonSchemaValidator(ObjectMapper objectMapper, CompiledSchemaCache compiledSchemaCache) {
        this.objectMapper = Objects.requireNonNull(objectMapper);
        this.compiledSchemaCache = Objects.requireNonNull(compiledSchemaCache);
        this.schemaRegistry = SchemaRegistry.withDefaultDialect(SpecificationVersion.DRAFT_2020_12);
    }

    public void validateDefinition(JsonNode definition, String field) {
        ValidationResult result = new ValidationResult();
        if (definition == null
                || definition.isNull()
                || !(definition.isObject() || definition.isBoolean())) {
            result.addError(field, SchemaValidationMessageKeys.INVALID);
        } else {
            try {
                compileDefinition(definition);
            } catch (RuntimeException exception) {
                result.addError(field, SchemaValidationMessageKeys.INVALID);
            }
        }
        if (result.hasErrors()) throw new ValidationException(result);
    }

    public void validate(
            String definition, JsonNode payload, String field, ValidationResult result) {
        validate(new ResourceSchema("RESOLVED", field, null, definition), payload, field, result);
    }

    public void validate(ResourceSchema resourceSchema, JsonNode payload) {
        ValidationResult result = new ValidationResult();
        validate(resourceSchema, payload, "", result);
        if (result.hasErrors()) throw new ValidationException(result);
    }

    public void validate(
            ResourceSchema resourceSchema,
            JsonNode payload,
            String field,
            ValidationResult result) {
        Objects.requireNonNull(result, "result");
        if (resourceSchema == null) {
            throw new PlatformConfigurationException(
                    SchemaValidationTechnicalErrors.publishedSchemaNotFound("resolved", "schema"));
        }
        Schema schema =
                compiledSchemaCache
                        .get(resourceSchema)
                        .orElseGet(
                                () -> {
                                    Schema compiled = parse(resourceSchema);
                                    compiledSchemaCache.put(resourceSchema, compiled);
                                    return compiled;
                                });
        if (payload == null || payload.isNull()) {
            result.addError(
                    field == null || field.isBlank() ? "request" : field,
                    SchemaValidationMessageKeys.INVALID);
            return;
        }
        List<Error> errors;
        try {
            errors =
                    schema.validate(
                            payload,
                            context ->
                                    context.executionConfig(
                                            config -> config.formatAssertionsEnabled(true)));
        } catch (SchemaException exception) {
            throw invalidPublishedSchema(resourceSchema, exception);
        }
        errors.forEach(
                error -> {
                    String relative = resolveField(error);
                    SchemaValidationErrorMapper.MappedValidationError mapped =
                            errorMapper.map(error, relative);
                    String mappedField = prefix(field, mapped.field());
                    Map<String, Object> parameters = new LinkedHashMap<>(mapped.parameters());
                    parameters.put("0", mappedField);
                    result.addError(
                            mappedField,
                            mapped.messageKey(),
                            Map.copyOf(parameters),
                            SchemaValidationMessageKeys.INVALID);
                });
    }

    private String prefix(String field, String relative) {
        if (field == null || field.isBlank()) return relative;
        if ("request".equals(relative)) return field;
        return field + (relative.startsWith("[") ? "" : ".") + relative;
    }

    private Schema compileDefinition(JsonNode definition) {
        Schema compiled = schemaRegistry.getSchema(definition);
        Schema metaSchema =
                schemaRegistry.getSchema(
                        SchemaLocation.of(compiled.getSchemaContext().getDialect().getId()));
        if (!metaSchema
                .validate(
                        definition,
                        context ->
                                context.executionConfig(
                                        config -> config.formatAssertionsEnabled(true)))
                .isEmpty()) {
            throw new IllegalArgumentException("Invalid JSON Schema definition");
        }
        return compiled;
    }

    private Schema parse(ResourceSchema resourceSchema) {
        if (resourceSchema.definition() == null || resourceSchema.definition().isBlank()) {
            throw new PlatformConfigurationException(
                    SchemaValidationTechnicalErrors.publishedSchemaInvalid(
                            resourceSchema.resourceType(),
                            resourceSchema.resourceCode(),
                            resourceSchema.schemaVersion()));
        }

        JsonNode schemaNode;
        try {
            schemaNode = objectMapper.readTree(resourceSchema.definition());
        } catch (Exception exception) {
            throw invalidPublishedSchema(resourceSchema, exception);
        }

        if (schemaNode == null || schemaNode.isNull()) {
            throw invalidPublishedSchema(resourceSchema);
        }

        try {
            return compileDefinition(schemaNode);
        } catch (Exception exception) {
            throw invalidPublishedSchema(resourceSchema, exception);
        }
    }

    private PlatformConfigurationException invalidPublishedSchema(ResourceSchema resourceSchema) {
        return new PlatformConfigurationException(
                SchemaValidationTechnicalErrors.publishedSchemaInvalid(
                        resourceSchema.resourceType(),
                        resourceSchema.resourceCode(),
                        resourceSchema.schemaVersion()));
    }

    private PlatformConfigurationException invalidPublishedSchema(
            ResourceSchema resourceSchema, Exception exception) {
        return new PlatformConfigurationException(
                SchemaValidationTechnicalErrors.publishedSchemaInvalid(
                        resourceSchema.resourceType(),
                        resourceSchema.resourceCode(),
                        resourceSchema.schemaVersion()),
                exception);
    }

    private String resolveField(Error error) {
        String instanceLocation =
                error.getInstanceLocation() == null ? "" : error.getInstanceLocation().toString();

        String field = appendJsonPointer(instanceLocation);
        String property = error.getProperty();

        if (property != null && !property.isBlank() && !fieldEndsWithProperty(field, property)) {
            field = appendProperty(field, property);
        }

        return field.isBlank() ? "request" : field;
    }

    private String appendJsonPointer(String instanceLocation) {
        if (instanceLocation == null
                || instanceLocation.isBlank()
                || "/".equals(instanceLocation)) {
            return "";
        }

        StringBuilder field = new StringBuilder();
        for (String token : instanceLocation.split("/")) {
            if (token.isBlank()) {
                continue;
            }

            String decodedToken = token.replace("~1", "/").replace("~0", "~");
            if (decodedToken.chars().allMatch(Character::isDigit)) {
                field.append('[').append(decodedToken).append(']');
            } else {
                if (!field.isEmpty()) {
                    field.append('.');
                }
                field.append(decodedToken);
            }
        }
        return field.toString();
    }

    private boolean fieldEndsWithProperty(String field, String property) {
        return field.equals(property)
                || field.endsWith("." + property)
                || field.endsWith("[" + property + "]");
    }

    private String appendProperty(String field, String property) {
        if (field == null || field.isBlank()) {
            return property;
        }
        return property.chars().allMatch(Character::isDigit)
                ? field + "[" + property + "]"
                : field + "." + property;
    }
}
