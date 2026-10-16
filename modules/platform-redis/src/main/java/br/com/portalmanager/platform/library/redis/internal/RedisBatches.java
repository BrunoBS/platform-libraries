package br.com.portalmanager.platform.library.redis.internal;

import br.com.portalmanager.platform.library.redis.serialization.RedisJsonCodec;

import org.springframework.data.redis.connection.ClusterSlotHashUtil;
import org.springframework.data.redis.connection.lettuce.LettuceConnectionFactory;

import java.util.*;
import java.util.function.Function;

/** Shared preparation/alignment only; transport remains in the sync/reactive stores. */
final class RedisBatches {
    private final RedisAccess access;
    private final RedisJsonCodec codec;
    private final boolean cluster;

    RedisBatches(RedisAccess access, RedisJsonCodec codec, Object connectionFactory) {
        this.access = access;
        this.codec = codec;
        this.cluster =
                connectionFactory instanceof LettuceConnectionFactory lettuce
                        ? lettuce.isClusterAware()
                        : connectionFactory != null;
    }

    Map<String, String> encode(Map<String, ?> entries) {
        if (entries == null) throw RedisErrors.invalid();
        if (entries.isEmpty()) return Map.of();
        access.batch(entries.size());
        Map<String, String> encoded = new LinkedHashMap<>();
        entries.forEach((key, value) -> encoded.put(access.key(key), codec.encode(value)));
        sameSlot(encoded.keySet());
        return encoded;
    }

    Map<String, String> keys(Collection<String> keys) {
        if (keys == null) throw RedisErrors.invalid();
        Map<String, String> result = new LinkedHashMap<>();
        for (String key : keys) {
            result.putIfAbsent(key, access.key(key));
            if (result.size() > access.properties().maxReadSize()) throw RedisErrors.invalid();
        }
        sameSlot(result.values());
        return result;
    }

    private void sameSlot(Collection<String> keys) {
        if (!cluster || keys.size() < 2) return;
        var iterator = keys.iterator();
        int slot = ClusterSlotHashUtil.calculateSlot(iterator.next());
        while (iterator.hasNext()) {
            if (ClusterSlotHashUtil.calculateSlot(iterator.next()) != slot)
                throw RedisErrors.invalid();
        }
    }

    <T> Map<String, T> decode(
            Map<String, String> keys, List<String> values, Function<String, T> decoder) {
        if (values == null || values.size() != keys.size())
            throw RedisErrors.error("unavailable", null);
        Map<String, T> result = new LinkedHashMap<>();
        int index = 0;
        for (String key : keys.keySet()) {
            String json = values.get(index++);
            if (json != null) result.put(key, decoder.apply(json));
        }
        return Collections.unmodifiableMap(result);
    }
}
