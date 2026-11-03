package br.com.portalmanager.platform.library.redis.internal.operations;

import br.com.portalmanager.platform.library.redis.internal.common.RedisAccess;
import br.com.portalmanager.platform.library.redis.internal.common.RedisErrors;
import br.com.portalmanager.platform.library.redis.internal.serialization.RedisJsonCodec;
import br.com.portalmanager.platform.library.redis.store.RedisStore;
import java.util.List;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;

/** Shared Redis dependencies and validation used by the operation implementations. */
final class RedisOperationContext {
    static final DefaultRedisScript<List> HASH_READ =
            new DefaultRedisScript<>(
                    """
                    if redis.call('HLEN',KEYS[1]) > tonumber(ARGV[1]) then return redis.error_reply('PLATFORM_REDIS_LIMIT') end
                    return redis.call('HGETALL',KEYS[1])
                    """,
                    List.class);
    static final DefaultRedisScript<List> SET_READ =
            new DefaultRedisScript<>(
                    """
                    if redis.call('SCARD',KEYS[1]) > tonumber(ARGV[1]) then return redis.error_reply('PLATFORM_REDIS_LIMIT') end
                    return redis.call('SMEMBERS',KEYS[1])
                    """,
                    List.class);
    static final DefaultRedisScript<Long> CAS =
            new DefaultRedisScript<>(
                    """
                    local old = redis.call('GET',KEYS[1])
                    if (ARGV[1] == 'absent' and not old) or (ARGV[1] == 'present' and old == ARGV[2]) then
                        redis.call('SET',KEYS[1],ARGV[3],'KEEPTTL')
                        return 1
                    end
                    return 0
                    """,
                    Long.class);
    static final DefaultRedisScript<Long> GUARDED_SET =
            new DefaultRedisScript<>(
                    "if redis.call('GET',KEYS[1]) ~= ARGV[1] then return 0 end "
                            + "redis.call('SET',KEYS[2],ARGV[2]); return 1",
                    Long.class);
    static final DefaultRedisScript<Long> GUARDED_DELETE =
            new DefaultRedisScript<>(
                    "if redis.call('GET',KEYS[1]) ~= ARGV[1] then return 0 end "
                            + "redis.call('DEL',KEYS[2]); return 1",
                    Long.class);
    static final DefaultRedisScript<Long> COMPARE_DELETE =
            new DefaultRedisScript<>(
                    "if redis.call('GET',KEYS[1]) == ARGV[1] then return redis.call('DEL',KEYS[1])"
                            + " end return 0",
                    Long.class);

    final StringRedisTemplate redis;
    final RedisJsonCodec codec;
    final RedisAccess access;
    final RedisStore store;

    RedisOperationContext(
            StringRedisTemplate redis, RedisJsonCodec codec, RedisAccess access, RedisStore store) {
        this.redis = redis;
        this.codec = codec;
        this.access = access;
        this.store = store;
    }

    void target(Class<?> type) {
        if (type == null) throw RedisErrors.invalid();
    }

    void finite(double value) {
        if (!Double.isFinite(value)) throw RedisErrors.invalid();
    }

    List<String> guardedKeys(String guardKey, String key) {
        String guard = access.key(guardKey);
        String target = access.key(key);
        if (guard.equals(target)) throw RedisErrors.invalid();
        return List.of(guard, target);
    }
}
