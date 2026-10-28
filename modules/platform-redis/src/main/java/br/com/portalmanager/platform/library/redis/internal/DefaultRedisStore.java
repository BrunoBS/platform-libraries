package br.com.portalmanager.platform.library.redis.internal;

import br.com.portalmanager.platform.library.redis.core.RedisStore;
import br.com.portalmanager.platform.library.redis.core.RedisKeyPage;
import br.com.portalmanager.platform.library.redis.core.RedisWrite;
import br.com.portalmanager.platform.library.redis.serialization.RedisJsonCodec;

import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import java.nio.charset.StandardCharsets;

import tools.jackson.core.type.TypeReference;

import java.time.Duration;
import java.util.ArrayList;
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
    private final StringRedisTemplate redis;
    private final RedisJsonCodec codec;
    private final RedisAccess access;
    private final RedisBatches batches;

    public DefaultRedisStore(StringRedisTemplate redis, RedisJsonCodec codec, RedisAccess access) {
        this.redis = redis;
        this.codec = codec;
        this.access = access;
        this.batches = new RedisBatches(access, codec, redis.getConnectionFactory());
    }

    public <T> void setAll(Map<String, T> entries) {
        var encoded = batches.encode(entries);
        if (encoded.isEmpty()) return;
        RedisErrors.call(
                () -> {
                    redis.opsForValue().multiSet(encoded);
                    return null;
                });
    }

    public <T> Map<String, T> getAll(Collection<String> keys, Class<T> type) {
        if (type == null) throw RedisErrors.invalid();
        return readAll(keys, json -> codec.decode(json, type));
    }

    public <T> Map<String, T> getAll(Collection<String> keys, TypeReference<T> type) {
        if (type == null) throw RedisErrors.invalid();
        return readAll(keys, json -> codec.decode(json, type));
    }

    private <T> Map<String, T> readAll(Collection<String> keys, Function<String, T> decoder) {
        var prepared = batches.keys(keys);
        if (prepared.isEmpty()) return Map.of();
        var values =
                RedisErrors.call(
                        () -> redis.opsForValue().multiGet(new ArrayList<>(prepared.values())));
        return batches.decode(prepared, values, decoder);
    }

    public <T> void set(String key, T value) {
        write(key, value).execute();
    }

    public <T> void set(String key, T value, Duration ttl) {
        write(key, value).ttl(ttl).execute();
    }

    public <T> Optional<T> get(String key, Class<T> type) {
        if (type == null) throw RedisErrors.invalid();
        return Optional.ofNullable(
                        RedisErrors.call(
                                () ->
                                        redis.execute(
                                                RedisReadScripts.GET,
                                                List.of(access.key(key)),
                                                Integer.toString(
                                                        access.properties().maxPayloadBytes()))))
                .map(json -> codec.decode(json, type));
    }

    public <T> Optional<T> get(String key, TypeReference<T> type) {
        if (type == null) throw RedisErrors.invalid();
        return Optional.ofNullable(
                        RedisErrors.call(
                                () ->
                                        redis.execute(
                                                RedisReadScripts.GET,
                                                List.of(access.key(key)),
                                                Integer.toString(
                                                        access.properties().maxPayloadBytes()))))
                .map(json -> codec.decode(json, type));
    }

    public boolean exists(String key) {
        return RedisErrors.call(() -> Boolean.TRUE.equals(redis.hasKey(access.key(key))));
    }

    public boolean delete(String key) {
        return RedisErrors.call(() -> Boolean.TRUE.equals(redis.delete(access.key(key))));
    }

    @Override
    public long deleteAll(Collection<String> keys) {
        Collection<String> physicalKeys = batches.deletionKeys(keys);
        if (physicalKeys.isEmpty()) return 0;
        Long removed = RedisErrors.call(() -> redis.delete(physicalKeys));
        if (removed == null) throw RedisErrors.error("unavailable", null);
        return removed;
    }

    @Override
    public RedisKeyPage scan(String prefix, String cursor, int count) {
        if (prefix == null) throw RedisErrors.invalid();
        requireCursor(cursor);
        access.limit(count);
        if (!prefix.isEmpty()) access.key(prefix);
        batches.requireSingleNodeScan();
        String physicalPrefix = access.prefix() + prefix;
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
                keys.add(physicalKey.substring(access.prefix().length()));
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
        Long millis =
                RedisErrors.call(() -> redis.getExpire(access.key(key), TimeUnit.MILLISECONDS));
        return millis == null || millis < 0
                ? Optional.empty()
                : Optional.of(Duration.ofMillis(millis));
    }

    public boolean expire(String key, Duration ttl) {
        return RedisErrors.call(
                () -> Boolean.TRUE.equals(redis.expire(access.key(key), access.duration(ttl))));
    }

    public boolean persist(String key) {
        return RedisErrors.call(() -> Boolean.TRUE.equals(redis.persist(access.key(key))));
    }

    public <T> boolean setIfAbsent(String key, T value) {
        return write(key, value).ifAbsent().execute();
    }

    public <T> boolean setIfAbsent(String key, T value, Duration ttl) {
        return write(key, value).ttl(ttl).ifAbsent().execute();
    }

    public <T> RedisWrite<T> write(String key, T value) {
        String physicalKey = access.key(key);
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
}
