package br.com.portalmanager.platform.library.redis.internal;

import br.com.portalmanager.platform.library.redis.core.RedisStore;
import br.com.portalmanager.platform.library.redis.core.RedisWrite;
import br.com.portalmanager.platform.library.redis.serialization.RedisJsonCodec;

import org.springframework.data.redis.core.StringRedisTemplate;

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
