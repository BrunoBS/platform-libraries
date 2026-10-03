package br.com.portalmanager.platform.library.audit.queue;

import br.com.portalmanager.platform.library.audit.config.PlatformAuditProperties;
import br.com.portalmanager.platform.library.audit.exception.AuditException;
import br.com.portalmanager.platform.library.audit.message.AuditMessageKeys;
import br.com.portalmanager.platform.library.audit.model.AuditEventRequest;
import org.springframework.data.redis.core.StringRedisTemplate;
import tools.jackson.databind.ObjectMapper;

public final class RedisAuditEventQueue implements AuditEventQueue {

    private final StringRedisTemplate redisTemplate;
    private final ObjectMapper objectMapper;
    private final String key;

    public RedisAuditEventQueue(StringRedisTemplate redisTemplate, ObjectMapper objectMapper, PlatformAuditProperties properties) {
        this.redisTemplate = redisTemplate;
        this.objectMapper = objectMapper;
        this.key = properties.getQueue().getKeyPrefix() + properties.getServiceName();
    }

    @Override
    public void save(AuditEventRequest event) {
        try {
            redisTemplate.opsForList().rightPush(key, objectMapper.writeValueAsString(event));
        } catch (Exception exception) {
            throw new AuditException(AuditMessageKeys.QUEUE_PERSIST_FAILED, exception);
        }
    }

    @Override
    public AuditEventRequest peek() {
        String value = redisTemplate.opsForList().index(key, 0);
        if (value == null || value.isBlank()) return null;
        try {
            return objectMapper.readValue(value, AuditEventRequest.class);
        } catch (Exception exception) {
            throw new AuditException(AuditMessageKeys.QUEUE_DESERIALIZE_FAILED, exception);
        }
    }

    @Override
    public void removeHead() { redisTemplate.opsForList().leftPop(key); }

    @Override
    public boolean hasPending() {
        Long size = redisTemplate.opsForList().size(key);
        return size != null && size > 0;
    }
}
