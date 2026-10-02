package br.com.portalmanager.platform.library.schemavalidation.cache;

import br.com.portalmanager.platform.library.schemavalidation.model.ResourceSchema;
import org.springframework.data.redis.core.StringRedisTemplate;
import tools.jackson.databind.ObjectMapper;

import java.time.Duration;
import java.util.Optional;

public final class RedisResourceSchemaCache implements ResourceSchemaCache {
    private static final String PREFIX = "platform:schema-validation:";

    private final StringRedisTemplate redis;
    private final ObjectMapper objectMapper;
    private final Duration ttl;

    public RedisResourceSchemaCache(StringRedisTemplate redis, ObjectMapper objectMapper, Duration ttl) {
        this.redis = redis;
        this.objectMapper = objectMapper;
        this.ttl = ttl;
    }

    @Override
    public Optional<ResourceSchema> get(String resourceType, String resourceCode) {
        String value = redis.opsForValue().get(key(resourceType, resourceCode));
        if (value == null || value.isBlank()) {
            return Optional.empty();
        }
        try {
            return Optional.of(objectMapper.readValue(value, ResourceSchema.class));
        } catch (Exception exception) {
            throw new IllegalStateException("Unable to deserialize cached resource schema", exception);
        }
    }

    @Override
    public void put(ResourceSchema schema) {
        try {
            redis.opsForValue().set(
                    key(schema.resourceType(), schema.resourceCode()),
                    objectMapper.writeValueAsString(schema),
                    ttl
            );
        } catch (Exception exception) {
            throw new IllegalStateException("Unable to serialize cached resource schema", exception);
        }
    }

    private String key(String resourceType, String resourceCode) {
        return PREFIX + resourceType + ":" + resourceCode;
    }
}
