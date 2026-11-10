package br.com.portalmanager.platform.library.redis.internal.operations;

import static br.com.portalmanager.platform.library.redis.internal.common.RedisErrors.call;

import br.com.portalmanager.platform.library.redis.api.operations.RedisSets;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

final class DefaultRedisSets implements RedisSets {
    private final RedisOperationContext context;

    DefaultRedisSets(RedisOperationContext context) { this.context = context; }

    public <T> boolean add(String key, T value) {
        return call(() -> context.redis.opsForSet().add(context.access.key(key), context.codec.encode(value)) > 0);
    }

    public <T> boolean remove(String key, T value) {
        return call(() -> context.redis.opsForSet().remove(context.access.key(key), context.codec.encode(value)) > 0);
    }

    public <T> boolean contains(String key, T value) {
        return call(
                () ->
                        Boolean.TRUE.equals(
                                context.redis.opsForSet()
                                        .isMember(context.access.key(key), context.codec.encode(value))));
    }

    public <T> Set<T> members(String key, Class<T> type, int limit) {
        context.target(type);
        context.access.limit(limit);
        List<?> entries =
                call(
                        () ->
                                context.redis.execute(
                                        RedisOperationContext.SET_READ,
                                        List.of(context.access.key(key)),
                                        Integer.toString(limit)));
        Set<T> values = new LinkedHashSet<>();
        for (Object entry : entries) values.add(context.codec.decode((String) entry, type));
        return Collections.unmodifiableSet(values);
    }

    public long count(String key) {
        return call(() -> context.redis.opsForSet().size(context.access.key(key)));
    }
}
