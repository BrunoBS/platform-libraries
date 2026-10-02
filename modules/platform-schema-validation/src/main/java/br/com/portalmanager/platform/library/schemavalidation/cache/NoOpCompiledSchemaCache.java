package br.com.portalmanager.platform.library.schemavalidation.cache;

import br.com.portalmanager.platform.library.schemavalidation.model.ResourceSchema;
import com.networknt.schema.Schema;

import java.util.Optional;

public final class NoOpCompiledSchemaCache implements CompiledSchemaCache {
    @Override
    public Optional<Schema> get(ResourceSchema resourceSchema) {
        return Optional.empty();
    }

    @Override
    public void put(ResourceSchema resourceSchema, Schema schema) {
    }
}
