package br.com.portalmanager.platform.library.schemavalidation.repository;

import br.com.portalmanager.platform.library.schemavalidation.model.ResourceSchema;

import java.util.Optional;

public interface ResourceSchemaRepository {
    Optional<ResourceSchema> find(String resourceType, String resourceCode);
}
