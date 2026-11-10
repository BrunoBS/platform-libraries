package br.com.portalmanager.platform.library.redis.internal.operations;

import static br.com.portalmanager.platform.library.redis.internal.common.RedisErrors.call;

import br.com.portalmanager.platform.library.redis.api.operations.RedisHashes;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

final class DefaultRedisHashes implements RedisHashes {
    private final RedisOperationContext context;

    DefaultRedisHashes(RedisOperationContext context) { this.context = context; }

    public <T> void put(String key, String field, T value) {
        call(
                () -> {
                    context.redis.opsForHash()
                            .put(context.access.key(key), context.access.text(field), context.codec.encode(value));
                    return null;
                });
    }

    public <T> Optional<T> get(String key, String field, Class<T> type) {
        context.target(type);
        Object json = call(() -> context.redis.opsForHash().get(context.access.key(key), context.access.text(field)));
        return json == null ? Optional.empty() : Optional.of(context.codec.decode((String) json, type));
    }

    public <T> Map<String, T> getAll(String key, Class<T> type, int limit) {
        context.target(type);
        context.access.limit(limit);
        List<?> entries =
                call(
                        () ->
                                context.redis.execute(
                                        RedisOperationContext.HASH_READ,
                                        List.of(context.access.key(key)),
                                        Integer.toString(limit)));
        Map<String, T> values = new LinkedHashMap<>();
        for (int index = 0; index < entries.size(); index += 2)
            values.put(
                    (String) entries.get(index),
                    context.codec.decode((String) entries.get(index + 1), type));
        return Collections.unmodifiableMap(values);
    }

    public boolean remove(String key, String field) {
        return call(() -> context.redis.opsForHash().delete(context.access.key(key), context.access.text(field)) > 0);
    }

    public boolean exists(String key, String field) {
        return call(() -> context.redis.opsForHash().hasKey(context.access.key(key), context.access.text(field)));
    }
}
