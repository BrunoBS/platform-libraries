package br.com.portalmanager.platform.library.schemavalidation.cache;

import br.com.portalmanager.platform.library.schemavalidation.model.ResourceSchema;
import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.networknt.schema.Schema;

import java.time.Duration;
import java.util.Optional;

public final class CaffeineCompiledSchemaCache implements CompiledSchemaCache {
    private final Cache<String, Schema> cache;

    public CaffeineCompiledSchemaCache(Duration ttl, long maximumSize) {
        this.cache = Caffeine.newBuilder()
                .expireAfterAccess(ttl)
                .maximumSize(maximumSize)
                .build();
    }

    @Override
    public Optional<Schema> get(ResourceSchema resourceSchema) {
        return Optional.ofNullable(cache.getIfPresent(key(resourceSchema)));
    }

    @Override
    public void put(ResourceSchema resourceSchema, Schema schema) {
        cache.put(key(resourceSchema), schema);
    }

    private String key(ResourceSchema schema) {
        return schema.resourceType() + ":" + schema.resourceCode() + ":" + schema.schemaVersion();
    }
}
