package br.com.portalmanager.platform.library.redis.internal.operations;

import static br.com.portalmanager.platform.library.redis.internal.common.RedisErrors.call;

import br.com.portalmanager.platform.library.redis.api.operations.RedisLists;
import java.util.List;
import java.util.Optional;

final class DefaultRedisLists implements RedisLists {
    private final RedisOperationContext context;

    DefaultRedisLists(RedisOperationContext context) { this.context = context; }

    public <T> long pushLeft(String key, T value) {
        return call(() -> context.redis.opsForList().leftPush(context.access.key(key), context.codec.encode(value)));
    }

    public <T> long pushRight(String key, T value) {
        return call(() -> context.redis.opsForList().rightPush(context.access.key(key), context.codec.encode(value)));
    }

    public <T> Optional<T> popLeft(String key, Class<T> type) {
        context.target(type);
        return Optional.ofNullable(call(() -> context.redis.opsForList().leftPop(context.access.key(key))))
                .map(json -> context.codec.decode(json, type));
    }

    public <T> Optional<T> popRight(String key, Class<T> type) {
        context.target(type);
        return Optional.ofNullable(call(() -> context.redis.opsForList().rightPop(context.access.key(key))))
                .map(json -> context.codec.decode(json, type));
    }

    public <T> List<T> range(String key, long start, long end, Class<T> type) {
        context.target(type);
        context.access.range(start, end);
        return call(() -> context.redis.opsForList().range(context.access.key(key), start, end)).stream()
                .map(json -> context.codec.decode(json, type))
                .toList();
    }

    public long length(String key) {
        return call(() -> context.redis.opsForList().size(context.access.key(key)));
    }

    public void trim(String key, long start, long end) {
        context.access.range(start, end);
        call(
                () -> {
                    context.redis.opsForList().trim(context.access.key(key), start, end);
                    return null;
                });
    }
}
