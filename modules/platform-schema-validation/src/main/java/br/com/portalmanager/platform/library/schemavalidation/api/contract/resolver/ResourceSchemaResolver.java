package br.com.portalmanager.platform.library.schemavalidation.api.contract.resolver;

import br.com.portalmanager.platform.library.schemavalidation.api.contract.model.ResourceSchema;

public interface ResourceSchemaResolver {
    ResourceSchema resolve(String resourceType, String resourceCode);
}
