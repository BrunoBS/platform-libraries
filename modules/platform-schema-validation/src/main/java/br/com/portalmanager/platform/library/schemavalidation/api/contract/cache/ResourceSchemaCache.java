package br.com.portalmanager.platform.library.schemavalidation.api.contract.cache;

import br.com.portalmanager.platform.library.schemavalidation.api.contract.model.ResourceSchema;

import java.util.Optional;

public interface ResourceSchemaCache {
    Optional<ResourceSchema> get(String resourceType, String resourceCode);
    void put(ResourceSchema schema);
}
