package br.com.portalmanager.platform.library.schemavalidation.api.contract.cache;

import br.com.portalmanager.platform.library.schemavalidation.api.contract.model.ResourceSchema;
import com.networknt.schema.Schema;

import java.util.Optional;

public interface CompiledSchemaCache {
    Optional<Schema> get(ResourceSchema resourceSchema);
    void put(ResourceSchema resourceSchema, Schema schema);
}
