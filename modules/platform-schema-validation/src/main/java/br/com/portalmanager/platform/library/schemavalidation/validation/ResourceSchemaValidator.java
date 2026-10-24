package br.com.portalmanager.platform.library.schemavalidation.validation;

import br.com.portalmanager.platform.library.schemavalidation.cache.CompiledSchemaCache;
import br.com.portalmanager.platform.library.schemavalidation.cache.NoOpCompiledSchemaCache;
import br.com.portalmanager.platform.library.schemavalidation.resolver.ResourceSchemaResolver;
import br.com.portalmanager.platform.library.schemavalidation.exception.ResourceSchemaNotFoundException;

import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

import java.util.Objects;

/**
 * Resolves the published resource contract; technical validation is shared with programmatic
 * callers.
 */
public class ResourceSchemaValidator implements SchemaValidator {
    private final ResourceSchemaResolver resolver;
    private final JsonSchemaValidator validator;

    public ResourceSchemaValidator(ResourceSchemaResolver resolver, ObjectMapper objectMapper) {
        this(resolver, new JsonSchemaValidator(objectMapper, new NoOpCompiledSchemaCache()));
    }

    public ResourceSchemaValidator(
            ResourceSchemaResolver resolver, ObjectMapper objectMapper, CompiledSchemaCache cache) {
        this(resolver, new JsonSchemaValidator(objectMapper, cache));
    }

    public ResourceSchemaValidator(ResourceSchemaResolver resolver, JsonSchemaValidator validator) {
        this.resolver = Objects.requireNonNull(resolver);
        this.validator = Objects.requireNonNull(validator);
    }

    @Override
    public void validate(String resourceType, String resourceCode, JsonNode payload) {
        validator.validate(resolver.resolve(resourceType, resourceCode), payload);
    }

    @Override
    public boolean validateIfPresent(String resourceType, String resourceCode, JsonNode payload) {
        try {
            validator.validate(resolver.resolve(resourceType, resourceCode), payload);
            return true;
        } catch (ResourceSchemaNotFoundException exception) {
            return false;
        }
    }
}
