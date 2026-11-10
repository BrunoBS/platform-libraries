package br.com.portalmanager.platform.library.redis.internal.store;

import br.com.portalmanager.platform.library.redis.internal.common.RedisAccess;
import br.com.portalmanager.platform.library.redis.internal.common.RedisErrors;
import br.com.portalmanager.platform.library.redis.internal.common.RedisReadScripts;

import br.com.portalmanager.platform.library.redis.api.store.RedisStore;
import br.com.portalmanager.platform.library.redis.api.store.RedisKeyPage;
import br.com.portalmanager.platform.library.redis.api.store.RedisWrite;
import br.com.portalmanager.platform.library.redis.internal.serialization.RedisJsonCodec;

import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import java.nio.charset.StandardCharsets;

import tools.jackson.core.type.TypeReference;

import java.time.Duration;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Function;

public final class DefaultRedisStore implements RedisStore {
    // Spring's cursor iterator does not expose a resumable single page. This bounded script
    // returns one SCAN reply through the existing template/serializer, without retaining a cursor.
    private static final DefaultRedisScript<List> SCAN = new DefaultRedisScript<>(
            "return redis.call('SCAN', ARGV[1], 'MATCH', ARGV[2], 'COUNT', ARGV[3])", List.class);
    private static final int MAX_CACHED_NAMESPACE_VIEWS = 64;
    private final StringRedisTemplate redis;
    private final RedisJsonCodec codec;
    private final RedisAccess access;
    private final RedisBatches batches;
    private final String namespace;
    private final Map<String, RedisStore> namespaceViews;

    public DefaultRedisStore(StringRedisTemplate redis, RedisJsonCodec codec, RedisAccess access) {
        this(
                redis,
                codec,
                access,
                new RedisBatches(access, codec, redis.getConnectionFactory()),
                null,
                new LinkedHashMap<>(16, 0.75f, true));
    }

    private DefaultRedisStore(
            StringRedisTemplate redis,
            RedisJsonCodec codec,
            RedisAccess access,
            RedisBatches batches,
            String namespace,
            Map<String, RedisStore> namespaceViews) {
        this.redis = redis;
        this.codec = codec;
        this.access = access;
        this.batches = batches;
        this.namespace = namespace;
        this.namespaceViews = namespaceViews;
    }

    @Override
    public RedisStore withNamespace(String namespace) {
        access.prefix(namespace); // validate before looking up a scoped view
        synchronized (namespaceViews) {
            RedisStore scoped = namespaceViews.get(namespace);
            if (scoped != null) return scoped;

            scoped = new DefaultRedisStore(redis, codec, access, batches, namespace, namespaceViews);
            namespaceViews.put(namespace, scoped);
            if (namespaceViews.size() > MAX_CACHED_NAMESPACE_VIEWS) {
                Iterator<String> eldest = namespaceViews.keySet().iterator();
                eldest.next();
                eldest.remove();
            }
            return scoped;
        }
    }

    public <T> void setAll(Map<String, T> entries) {
        var encoded = namespace == null ? batches.encode(entries) : batches.encode(namespace, entries);
        if (encoded.isEmpty()) return;
        RedisErrors.call(() -> { redis.opsForValue().multiSet(encoded); return null; });
    }

    public <T> void setAll(String namespace, Map<String, T> entries) {
        var encoded = batches.encode(namespace, entries);
        if (encoded.isEmpty()) return;
        RedisErrors.call(
                () -> {
                    redis.opsForValue().multiSet(encoded);
                    return null;
                });
    }

    public <T> Map<String, T> getAll(Collection<String> keys, Class<T> type) {
        if (type == null) throw RedisErrors.invalid();
        return readAll(namespace, keys, json -> codec.decode(json, type));
    }

    public <T> Map<String, T> getAll(String namespace, Collection<String> keys, Class<T> type) {
        if (type == null) throw RedisErrors.invalid();
        return readAll(namespace, keys, json -> codec.decode(json, type));
    }

    public <T> Map<String, T> getAll(Collection<String> keys, TypeReference<T> type) {
        if (type == null) throw RedisErrors.invalid();
        return readAll(namespace, keys, json -> codec.decode(json, type));
    }

    public <T> Map<String, T> getAll(String namespace, Collection<String> keys, TypeReference<T> type) {
        if (type == null) throw RedisErrors.invalid();
        return readAll(namespace, keys, json -> codec.decode(json, type));
    }

    private <T> Map<String, T> readAll(Collection<String> keys, Function<String, T> decoder) {
        var prepared = namespace == null ? batches.keys(keys) : batches.keys(namespace, keys);
        if (prepared.isEmpty()) return Map.of();
        var values = RedisErrors.call(() -> redis.opsForValue().multiGet(new ArrayList<>(prepared.values())));
        return batches.decode(prepared, values, decoder);
    }

    private <T> Map<String, T> readAll(String namespace, Collection<String> keys, Function<String, T> decoder) {
        var prepared = batches.keys(namespace, keys);
        if (prepared.isEmpty()) return Map.of();
        var values =
                RedisErrors.call(
                        () -> redis.opsForValue().multiGet(new ArrayList<>(prepared.values())));
        return batches.decode(prepared, values, decoder);
    }

    public <T> void set(String key, T value) {
        write(key, value).execute();
    }

    public <T> void set(String namespace, String key, T value) {
        write(namespace, key, value).execute();
    }

    public <T> void set(String key, T value, Duration ttl) {
        write(key, value).ttl(ttl).execute();
    }

    public <T> void setWithTtl(String namespace, String key, T value, Duration ttl) {
        write(namespace, key, value).ttl(ttl).execute();
    }

    public <T> Optional<T> get(String key, Class<T> type) {
        return getValue(key, type, namespace);
    }

    public <T> Optional<T> get(String namespace, String key, Class<T> type) {
        return getValue(key, type, namespace);
    }

    private <T> Optional<T> getValue(String key, Class<T> type, String namespace) {
        if (type == null) throw RedisErrors.invalid();
        return Optional.ofNullable(
                        RedisErrors.call(
                                () ->
                                        redis.execute(
                                                RedisReadScripts.GET,
                                                List.of(physicalKey(key, namespace)),
                                                Integer.toString(
                                                        access.properties().limits().maxPayloadBytes()))))
                .map(json -> codec.decode(json, type));
    }

    public <T> Optional<T> get(String key, TypeReference<T> type) {
        return getValue(key, type, namespace);
    }

    public <T> Optional<T> get(String namespace, String key, TypeReference<T> type) {
        return getValue(key, type, namespace);
    }

    private <T> Optional<T> getValue(String key, TypeReference<T> type, String namespace) {
        if (type == null) throw RedisErrors.invalid();
        return Optional.ofNullable(
                        RedisErrors.call(
                                () ->
                                        redis.execute(
                                                RedisReadScripts.GET,
                                                List.of(physicalKey(key, namespace)),
                                                Integer.toString(
                                                        access.properties().limits().maxPayloadBytes()))))
                .map(json -> codec.decode(json, type));
    }

    public boolean exists(String key) {
        return RedisErrors.call(() -> Boolean.TRUE.equals(redis.hasKey(physicalKey(key, namespace))));
    }

    public boolean exists(String namespace, String key) {
        return RedisErrors.call(() -> Boolean.TRUE.equals(redis.hasKey(physicalKey(key, namespace))));
    }

    public boolean delete(String key) {
        return RedisErrors.call(() -> Boolean.TRUE.equals(redis.delete(physicalKey(key, namespace))));
    }

    public boolean delete(String namespace, String key) {
        return RedisErrors.call(() -> Boolean.TRUE.equals(redis.delete(physicalKey(key, namespace))));
    }

    @Override
    public long deleteAll(Collection<String> keys) {
        return deleteAllWithNamespace(keys, namespace);
    }

    public long deleteAll(String namespace, Collection<String> keys) {
        return deleteAllWithNamespace(keys, namespace);
    }

    private long deleteAllWithNamespace(Collection<String> keys, String namespace) {
        Collection<String> physicalKeys = batches.deletionKeys(namespace, keys);
        if (physicalKeys.isEmpty()) return 0;
        Long removed = RedisErrors.call(() -> redis.delete(physicalKeys));
        if (removed == null) throw RedisErrors.error("unavailable", null);
        return removed;
    }

    @Override
    public RedisKeyPage scan(String prefix, String cursor, int count) {
        return scanWithNamespace(prefix, cursor, count, namespace);
    }

    public RedisKeyPage scan(String namespace, String prefix, String cursor, int count) {
        return scanWithNamespace(prefix, cursor, count, namespace);
    }

    private RedisKeyPage scanWithNamespace(String prefix, String cursor, int count, String namespace) {
        if (prefix == null) throw RedisErrors.invalid();
        requireCursor(cursor);
        access.limit(count);
        if (!prefix.isEmpty()) physicalKey(prefix, namespace);
        batches.requireSingleNodeScan();
        String scopePrefix = namespace == null ? access.prefix() : access.prefix(namespace);
        String physicalPrefix = scopePrefix + prefix;
        String pattern = physicalPrefix.replace("\\", "\\\\").replace("*", "\\*")
                .replace("?", "\\?").replace("[", "\\[").replace("]", "\\]") + "*";
        List<?> response = RedisErrors.call(() -> redis.execute(SCAN, List.of(), cursor, pattern, Integer.toString(count)));
        if (response == null || response.size() != 2 || !(response.get(1) instanceof List<?> values)) {
            throw RedisErrors.error("unavailable", null);
        }
        String next = scanText(response.get(0));
        try { requireCursor(next); }
        catch (RuntimeException malformed) { throw RedisErrors.error("unavailable", null); }
        List<String> keys = new ArrayList<>();
        for (Object value : values) {
            String physicalKey = scanText(value);
            if (physicalKey.startsWith(physicalPrefix)) {
                keys.add(physicalKey.substring(scopePrefix.length()));
            }
        }
        return new RedisKeyPage(next, keys);
    }

    private static void requireCursor(String cursor) {
        if (cursor == null || !cursor.matches("0|[1-9][0-9]{0,19}")) throw RedisErrors.invalid();
        try { Long.parseUnsignedLong(cursor); }
        catch (NumberFormatException invalid) { throw RedisErrors.invalid(); }
    }

    private static String scanText(Object value) {
        if (value instanceof String text) return text;
        if (value instanceof byte[] bytes) return new String(bytes, StandardCharsets.UTF_8);
        throw RedisErrors.error("unavailable", null);
    }

    public Optional<Duration> ttl(String key) {
        return ttlWithNamespace(key, namespace);
    }

    public Optional<Duration> ttl(String namespace, String key) {
        return ttlWithNamespace(key, namespace);
    }

    private Optional<Duration> ttlWithNamespace(String key, String namespace) {
        Long millis =
                RedisErrors.call(() -> redis.getExpire(physicalKey(key, namespace), TimeUnit.MILLISECONDS));
        return millis == null || millis < 0
                ? Optional.empty()
                : Optional.of(Duration.ofMillis(millis));
    }

    public boolean expire(String key, Duration ttl) {
        return RedisErrors.call(() -> Boolean.TRUE.equals(redis.expire(physicalKey(key, namespace), access.duration(ttl))));
    }

    public boolean expire(String namespace, String key, Duration ttl) {
        return RedisErrors.call(
                () -> Boolean.TRUE.equals(redis.expire(physicalKey(key, namespace), access.duration(ttl))));
    }

    public boolean persist(String key) {
        return RedisErrors.call(() -> Boolean.TRUE.equals(redis.persist(physicalKey(key, namespace))));
    }

    public boolean persist(String namespace, String key) {
        return RedisErrors.call(() -> Boolean.TRUE.equals(redis.persist(physicalKey(key, namespace))));
    }

    public <T> boolean setIfAbsent(String key, T value) {
        return write(key, value).ifAbsent().execute();
    }

    public <T> boolean setIfAbsent(String namespace, String key, T value) {
        return write(namespace, key, value).ifAbsent().execute();
    }

    public <T> boolean setIfAbsent(String key, T value, Duration ttl) {
        return write(key, value).ttl(ttl).ifAbsent().execute();
    }

    public <T> boolean setIfAbsentWithTtl(String namespace, String key, T value, Duration ttl) {
        return write(namespace, key, value).ttl(ttl).ifAbsent().execute();
    }

    public <T> RedisWrite<T> write(String key, T value) {
        return createWrite(physicalKey(key, namespace), value);
    }

    public <T> RedisWrite<T> write(String namespace, String key, T value) {
        return createWrite(physicalKey(key, namespace), value);
    }

    private <T> RedisWrite<T> createWrite(String physicalKey, T value) {
        String json = codec.encode(value);
        return new RedisWrite<T>() {
            private final AtomicBoolean used = new AtomicBoolean();
            private Duration ttl;
            private boolean nx;

            private void unused() {
                if (used.get()) throw RedisErrors.error("builder.used", null);
            }

            public synchronized RedisWrite<T> ttl(Duration duration) {
                unused();
                ttl = access.duration(duration);
                return this;
            }

            public synchronized RedisWrite<T> ifAbsent() {
                unused();
                nx = true;
                return this;
            }

            public synchronized boolean execute() {
                if (!used.compareAndSet(false, true)) throw RedisErrors.error("builder.used", null);
                return RedisErrors.call(
                        () -> {
                            if (nx)
                                return Boolean.TRUE.equals(
                                        ttl == null
                                                ? redis.opsForValue().setIfAbsent(physicalKey, json)
                                                : redis.opsForValue()
                                                        .setIfAbsent(physicalKey, json, ttl));
                            if (ttl == null) redis.opsForValue().set(physicalKey, json);
                            else redis.opsForValue().set(physicalKey, json, ttl);
                            return true;
                        });
            }
        };
    }

    private String physicalKey(String key, String namespace) {
        return namespace == null ? access.key(key) : access.key(namespace, key);
    }
}
