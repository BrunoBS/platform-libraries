package br.com.portalmanager.platform.library.schemavalidation.validation;

import br.com.portalmanager.platform.library.messaging.exception.ValidationException;
import br.com.portalmanager.platform.library.messaging.exception.PlatformConfigurationException;
import br.com.portalmanager.platform.library.messaging.validation.ValidationResult;
import br.com.portalmanager.platform.library.schemavalidation.message.SchemaValidationMessageKeys;
import br.com.portalmanager.platform.library.schemavalidation.message.SchemaValidationTechnicalErrors;
import br.com.portalmanager.platform.library.schemavalidation.model.ResourceSchema;
import br.com.portalmanager.platform.library.schemavalidation.resolver.ResourceSchemaResolver;
import com.networknt.schema.Error;
import com.networknt.schema.Schema;
import com.networknt.schema.SchemaRegistry;
import com.networknt.schema.SpecificationVersion;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;


public class ResourceSchemaValidator implements SchemaValidator {

    private final ResourceSchemaResolver resolver;
    private final ObjectMapper objectMapper;
    private final SchemaRegistry schemaRegistry;
    private final SchemaValidationErrorMapper errorMapper = new SchemaValidationErrorMapper();

    public ResourceSchemaValidator(
            ResourceSchemaResolver resolver,
            ObjectMapper objectMapper
    ) {
        this.resolver = resolver;
        this.objectMapper = objectMapper;
        this.schemaRegistry = SchemaRegistry.withDefaultDialect(SpecificationVersion.DRAFT_2020_12);
    }

    @Override
    public void validate(String resourceType, String resourceCode, JsonNode payload) {
        ValidationResult result = new ValidationResult();

        if (payload == null || payload.isNull()) {
            result.addError("request", SchemaValidationMessageKeys.INVALID);
            throw new ValidationException(result);
        }

        ResourceSchema resourceSchema = resolver.resolve(resourceType, resourceCode);
        Schema schema = parse(resourceSchema);

        schema.validate(
                payload,
                executionContext -> executionContext.executionConfig(
                        executionConfig -> executionConfig.formatAssertionsEnabled(true)
                )
        ).forEach(error -> {
            String field = resolveField(error);
            SchemaValidationErrorMapper.MappedValidationError mappedError = errorMapper.map(error, field);
            result.addError(
                    mappedError.field(),
                    mappedError.messageKey(),
                    mappedError.parameters(),
                    SchemaValidationMessageKeys.INVALID
            );
        });

        if (result.hasErrors()) {
            throw new ValidationException(result);
        }
    }

    private Schema parse(ResourceSchema resourceSchema) {
        if (resourceSchema.definition() == null || resourceSchema.definition().isBlank()) {
            throw new PlatformConfigurationException(
                    SchemaValidationTechnicalErrors.publishedSchemaInvalid(
                            resourceSchema.resourceType(),
                            resourceSchema.resourceCode(),
                            resourceSchema.schemaVersion()
                    )
            );
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
            return schemaRegistry.getSchema(schemaNode);
        } catch (Exception exception) {
            throw invalidPublishedSchema(resourceSchema, exception);
        }
    }

    private PlatformConfigurationException invalidPublishedSchema(ResourceSchema resourceSchema) {
        return new PlatformConfigurationException(
                SchemaValidationTechnicalErrors.publishedSchemaInvalid(
                        resourceSchema.resourceType(),
                        resourceSchema.resourceCode(),
                        resourceSchema.schemaVersion()
                )
        );
    }

    private PlatformConfigurationException invalidPublishedSchema(
            ResourceSchema resourceSchema,
            Exception exception
    ) {
        return new PlatformConfigurationException(
                SchemaValidationTechnicalErrors.publishedSchemaInvalid(
                        resourceSchema.resourceType(),
                        resourceSchema.resourceCode(),
                        resourceSchema.schemaVersion()
                ),
                exception
        );
    }

    private String resolveField(Error error) {
        String instanceLocation = error.getInstanceLocation() == null
                ? ""
                : error.getInstanceLocation().toString();

        String field = appendJsonPointer(instanceLocation);
        String property = error.getProperty();

        if (property != null && !property.isBlank() && !fieldEndsWithProperty(field, property)) {
            field = appendProperty(field, property);
        }

        return field.isBlank() ? "request" : field;
    }

    private String appendJsonPointer(String instanceLocation) {
        if (instanceLocation == null || instanceLocation.isBlank() || "/".equals(instanceLocation)) {
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
