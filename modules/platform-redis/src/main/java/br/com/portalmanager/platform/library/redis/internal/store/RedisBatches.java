package br.com.portalmanager.platform.library.redis.internal.store;

import br.com.portalmanager.platform.library.redis.internal.common.RedisAccess;
import br.com.portalmanager.platform.library.redis.internal.common.RedisErrors;
import br.com.portalmanager.platform.library.redis.internal.serialization.RedisJsonCodec;

import org.springframework.data.redis.connection.ClusterSlotHashUtil;
import org.springframework.data.redis.connection.lettuce.LettuceConnectionFactory;

import java.util.*;
import java.util.function.Function;

/** Shared batch preparation and alignment for the Redis Store. */
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
        return encode(null, entries);
    }

    Map<String, String> encode(String namespace, Map<String, ?> entries) {
        if (entries == null) throw RedisErrors.invalid();
        if (entries.isEmpty()) return Map.of();
        access.batch("setAll", entries.size(), access.properties().limits().maxBatchSize());
        Map<String, String> encoded = new LinkedHashMap<>();
        entries.forEach((key, value) -> encoded.put(key(namespace, key), codec.encode(value)));
        sameSlot(encoded.keySet());
        return encoded;
    }

    Map<String, String> keys(Collection<String> keys) {
        return keys(null, keys);
    }

    Map<String, String> keys(String namespace, Collection<String> keys) {
        if (keys == null) throw RedisErrors.invalid();
        Map<String, String> result = new LinkedHashMap<>();
        for (String key : keys) {
            result.putIfAbsent(key, key(namespace, key));
            if (result.size() > access.properties().limits().maxBatchSize())
                throw RedisErrors.batchLimit(
                        "getAll", keys.size(), access.properties().limits().maxBatchSize());
            if (result.size() > access.properties().limits().maxReadSize())
                throw RedisErrors.batchLimit(
                        "getAll", keys.size(), access.properties().limits().maxReadSize());
        }
        sameSlot(result.values());
        return result;
    }

    Collection<String> deletionKeys(Collection<String> keys) {
        return deletionKeys(null, keys);
    }

    Collection<String> deletionKeys(String namespace, Collection<String> keys) {
        if (keys == null) throw RedisErrors.invalid();
        if (keys.isEmpty()) return List.of();
        access.batch("deleteAll", keys.size(), access.properties().limits().maxBatchSize());
        Set<String> physicalKeys = new LinkedHashSet<>();
        for (String key : keys) physicalKeys.add(key(namespace, key));
        sameSlot(physicalKeys);
        return List.copyOf(physicalKeys);
    }

    private String key(String namespace, String key) {
        return namespace == null ? access.key(key) : access.key(namespace, key);
    }

    void requireSingleNodeScan() {
        if (cluster) throw RedisErrors.configuration();
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
