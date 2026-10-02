package br.com.portalmanager.platform.library.schemavalidation.cache;

import br.com.portalmanager.platform.library.schemavalidation.model.ResourceSchema;

import java.util.Optional;

public final class NoOpResourceSchemaCache implements ResourceSchemaCache {
    @Override
    public Optional<ResourceSchema> get(String resourceType, String resourceCode) {
        return Optional.empty();
    }

    @Override
    public void put(ResourceSchema schema) {
    }
}
