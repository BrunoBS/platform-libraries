package br.com.portalmanager.platform.library.redis.internal.operations;

import static br.com.portalmanager.platform.library.redis.internal.common.RedisErrors.call;

import br.com.portalmanager.platform.library.redis.operations.RedisSortedSets;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.Optional;
import java.util.Set;

final class DefaultRedisSortedSets implements RedisSortedSets {
    private final RedisOperationContext context;

    DefaultRedisSortedSets(RedisOperationContext context) { this.context = context; }

    public <T> boolean add(String key, T value, double score) {
        context.finite(score);
        return call(
                () ->
                        Boolean.TRUE.equals(
                                context.redis.opsForZSet()
                                        .add(context.access.key(key), context.codec.encode(value), score)));
    }

    public <T> boolean remove(String key, T value) {
        return call(() -> context.redis.opsForZSet().remove(context.access.key(key), context.codec.encode(value)) > 0);
    }

    public <T> Optional<Double> score(String key, T value) {
        return Optional.ofNullable(
                call(() -> context.redis.opsForZSet().score(context.access.key(key), context.codec.encode(value))));
    }

    public <T> double incrementScore(String key, T value, double delta) {
        context.finite(delta);
        return call(
                () ->
                        context.redis.opsForZSet()
                                .incrementScore(context.access.key(key), context.codec.encode(value), delta));
    }

    public <T> Set<T> range(String key, long start, long end, Class<T> type) {
        context.target(type);
        context.access.range(start, end);
        Set<T> values = new LinkedHashSet<>();
        for (String json : call(() -> context.redis.opsForZSet().range(context.access.key(key), start, end)))
            values.add(context.codec.decode(json, type));
        return Collections.unmodifiableSet(values);
    }

    public <T> Optional<Long> rank(String key, T value) {
        return Optional.ofNullable(
                call(() -> context.redis.opsForZSet().rank(context.access.key(key), context.codec.encode(value))));
    }

    public long count(String key) {
        return call(() -> context.redis.opsForZSet().size(context.access.key(key)));
    }
}
