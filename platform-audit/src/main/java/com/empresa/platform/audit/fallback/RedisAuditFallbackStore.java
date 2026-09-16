package com.empresa.platform.audit.fallback;

import com.empresa.platform.audit.config.PlatformAuditProperties;
import com.empresa.platform.audit.model.AuditEventRequest;
import org.springframework.data.redis.core.StringRedisTemplate;
import tools.jackson.databind.ObjectMapper;

public final class RedisAuditFallbackStore implements AuditFallbackStore {

    private final StringRedisTemplate redisTemplate;
    private final ObjectMapper objectMapper;
    private final String key;

    public RedisAuditFallbackStore(
            StringRedisTemplate redisTemplate,
            ObjectMapper objectMapper,
            PlatformAuditProperties properties
    ) {
        this.redisTemplate = redisTemplate;
        this.objectMapper = objectMapper;
        this.key = properties.getFallback().getKeyPrefix() + properties.getServiceName();
    }

    @Override
    public void save(AuditEventRequest event) {
        try {
            redisTemplate.opsForList().rightPush(key, objectMapper.writeValueAsString(event));
        } catch (Exception exception) {
            throw new IllegalStateException("Could not persist audit event in Redis fallback", exception);
        }
    }

    @Override
    public AuditEventRequest peek() {
        String value = redisTemplate.opsForList().index(key, 0);
        if (value == null || value.isBlank()) {
            return null;
        }

        try {
            return objectMapper.readValue(value, AuditEventRequest.class);
        } catch (Exception exception) {
            throw new IllegalStateException("Could not deserialize audit event from Redis fallback", exception);
        }
    }

    @Override
    public void removeHead() {
        redisTemplate.opsForList().leftPop(key);
    }

    @Override
    public boolean hasPending() {
        Long size = redisTemplate.opsForList().size(key);
        return size != null && size > 0;
    }
}
