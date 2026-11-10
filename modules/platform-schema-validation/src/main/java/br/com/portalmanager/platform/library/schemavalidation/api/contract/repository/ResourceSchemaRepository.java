package br.com.portalmanager.platform.library.schemavalidation.api.contract.repository;

import br.com.portalmanager.platform.library.schemavalidation.api.contract.model.ResourceSchema;

import java.util.Optional;

public interface ResourceSchemaRepository {
    Optional<ResourceSchema> find(String resourceType, String resourceCode);
}
