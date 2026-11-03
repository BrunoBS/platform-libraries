package br.com.portalmanager.platform.library.redis.internal.operations;

import static br.com.portalmanager.platform.library.redis.internal.common.RedisErrors.call;

import br.com.portalmanager.platform.library.redis.internal.common.RedisErrors;
import br.com.portalmanager.platform.library.redis.operations.RedisAtomic;
import java.util.List;

final class DefaultRedisAtomic implements RedisAtomic {
    private final RedisOperationContext context;

    DefaultRedisAtomic(RedisOperationContext context) { this.context = context; }

    public <T> boolean setIfValue(String guardKey, Object expectedGuard, String key, T value) {
        var keys = context.guardedKeys(guardKey, key);
        String expected = context.codec.encode(expectedGuard);
        String json = context.codec.encode(value);
        return Long.valueOf(1)
                .equals(call(() -> context.redis.execute(RedisOperationContext.GUARDED_SET, keys, expected, json)));
    }

    public boolean deleteIfValue(String guardKey, Object expectedGuard, String key) {
        var keys = context.guardedKeys(guardKey, key);
        String expected = context.codec.encode(expectedGuard);
        return Long.valueOf(1)
                .equals(call(() -> context.redis.execute(RedisOperationContext.GUARDED_DELETE, keys, expected)));
    }

    public boolean compareAndDelete(String key, Object expected) {
        String physical = context.access.key(key);
        String json = context.codec.encode(expected);
        return Long.valueOf(1)
                .equals(call(() -> context.redis.execute(RedisOperationContext.COMPARE_DELETE, List.of(physical), json)));
    }

    public long increment(String key, long delta) {
        if (delta < 1) throw RedisErrors.invalid();
        return call(() -> context.redis.opsForValue().increment(context.access.key(key), delta));
    }

    public long decrement(String key, long delta) {
        if (delta < 1) throw RedisErrors.invalid();
        return call(() -> context.redis.opsForValue().decrement(context.access.key(key), delta));
    }

    public <T> boolean setIfAbsent(String key, T value) {
        return context.store.setIfAbsent(key, value);
    }

    public <T> boolean compareAndSet(String key, T expected, T replacement) {
        String expectedJson = expected == null ? "" : context.codec.encode(expected);
        String json = context.codec.encode(replacement);
        return call(
                () ->
                        Long.valueOf(1)
                                .equals(
                                        context.redis.execute(
                                                RedisOperationContext.CAS,
                                                List.of(context.access.key(key)),
                                                expected == null ? "absent" : "present",
                                                expectedJson,
                                                json)));
    }
}
