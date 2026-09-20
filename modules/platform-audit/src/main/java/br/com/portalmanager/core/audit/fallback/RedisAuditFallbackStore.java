package br.com.portalmanager.core.audit.fallback;

import br.com.portalmanager.core.audit.config.PlatformAuditProperties;
import br.com.portalmanager.core.audit.exception.AuditException;
import br.com.portalmanager.core.audit.message.AuditMessageKeys;
import br.com.portalmanager.core.audit.model.AuditEventRequest;
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
            throw new AuditException(AuditMessageKeys.FALLBACK_PERSIST_FAILED, exception);
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
            throw new AuditException(AuditMessageKeys.FALLBACK_DESERIALIZE_FAILED, exception);
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
