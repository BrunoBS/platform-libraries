package br.com.portalmanager.platform.library.schemavalidation.resolver;

import br.com.portalmanager.platform.library.schemavalidation.config.PlatformSchemaValidationProperties;
import br.com.portalmanager.platform.library.schemavalidation.model.ResourceSchema;
import br.com.portalmanager.platform.library.schemavalidation.repository.ResourceSchemaRepository;

public class DefaultResourceSchemaResolver implements ResourceSchemaResolver {

    private final ResourceSchemaRepository repository;
    private final PlatformSchemaValidationProperties properties;

    public DefaultResourceSchemaResolver(
            ResourceSchemaRepository repository,
            PlatformSchemaValidationProperties properties
    ) {
        this.repository = repository;
        this.properties = properties;
    }

    @Override
    public ResourceSchema resolve(String resourceType, String resourceCode) {
        String type = requireText(resourceType, "resourceType");
        String code = requireText(resourceCode, "resourceCode");

        return repository.find(type, code)
                .or(() -> repository.find(type, properties.getFallbackCode()))
                .orElseThrow(() -> new IllegalStateException(
                        "No published resource schema found for " + type + "/" + code
                ));
    }

    private String requireText(String value, String field) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(field + " is required");
        }
        return value.trim();
    }
}
