package br.com.portalmanager.platform.library.audit.recovery;

import br.com.portalmanager.platform.library.audit.config.PlatformAuditProperties;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;

import java.util.Collections;
import java.util.Optional;
import java.util.UUID;

public final class RedisAuditRecoveryLock implements AuditRecoveryLock {

    private static final DefaultRedisScript<Long> RELEASE_SCRIPT = new DefaultRedisScript<>(
            "if redis.call('get', KEYS[1]) == ARGV[1] then "
                    + "return redis.call('del', KEYS[1]) "
                    + "else return 0 end",
            Long.class
    );

    private final StringRedisTemplate redisTemplate;
    private final String lockKey;
    private final PlatformAuditProperties properties;

    public RedisAuditRecoveryLock(
            StringRedisTemplate redisTemplate,
            PlatformAuditProperties properties
    ) {
        this.redisTemplate = redisTemplate;
        this.properties = properties;
        this.lockKey = properties.getQueue().getLock().getKeyPrefix()
                + properties.getServiceName();
    }

    @Override
    public Optional<String> tryAcquire() {
        String token = UUID.randomUUID().toString();
        Boolean acquired = redisTemplate.opsForValue().setIfAbsent(
                lockKey,
                token,
                properties.getQueue().getLock().getTtl()
        );
        return Boolean.TRUE.equals(acquired)
                ? Optional.of(token)
                : Optional.empty();
    }

    @Override
    public void release(String token) {
        if (token == null || token.isBlank()) {
            return;
        }
        redisTemplate.execute(
                RELEASE_SCRIPT,
                Collections.singletonList(lockKey),
                token
        );
    }
}
