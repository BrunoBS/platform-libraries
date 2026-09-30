package br.com.portalmanager.platform.library.schemavalidation.repository;

import br.com.portalmanager.platform.library.schemavalidation.model.ResourceSchema;

import java.util.Optional;

public final class NoOpResourceSchemaRepository implements ResourceSchemaRepository {

    @Override
    public Optional<ResourceSchema> find(String resourceType, String resourceCode) {
        return Optional.empty();
    }
}
