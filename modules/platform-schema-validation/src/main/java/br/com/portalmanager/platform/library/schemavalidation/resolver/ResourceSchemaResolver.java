package br.com.portalmanager.platform.library.schemavalidation.resolver;

import br.com.portalmanager.platform.library.schemavalidation.model.ResourceSchema;

public interface ResourceSchemaResolver {
    ResourceSchema resolve(String resourceType, String resourceCode);
}
