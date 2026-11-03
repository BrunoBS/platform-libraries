package br.com.portalmanager.platform.library.redis.internal;

import br.com.portalmanager.platform.library.redis.core.AsyncRedisStore;
import br.com.portalmanager.platform.library.redis.serialization.RedisJsonCodec;

import org.springframework.data.redis.core.ReactiveStringRedisTemplate;

import reactor.core.publisher.Mono;

import tools.jackson.core.type.TypeReference;

import java.time.Duration;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.function.Function;
import java.util.function.Supplier;

public final class DefaultAsyncRedisStore implements AsyncRedisStore {
    private final ReactiveStringRedisTemplate redis;
    private final RedisJsonCodec codec;
    private final RedisAccess access;
    private final RedisBatches batches;
    private final String namespace;

    public DefaultAsyncRedisStore(
            ReactiveStringRedisTemplate redis, RedisJsonCodec codec, RedisAccess access) {
        this(redis, codec, access, null);
    }

    private DefaultAsyncRedisStore(
            ReactiveStringRedisTemplate redis, RedisJsonCodec codec, RedisAccess access, String namespace) {
        this.redis = redis;
        this.codec = codec;
        this.access = access;
        this.batches = new RedisBatches(access, codec, redis.getConnectionFactory());
        this.namespace = namespace;
    }

    @Override
    public AsyncRedisStore withNamespace(String namespace) {
        access.prefix(namespace); // validate before creating the scoped view
        return new DefaultAsyncRedisStore(redis, codec, access, namespace);
    }

    private <T> CompletionStage<T> run(Supplier<Mono<T>> operation) {
        try {
            return operation.get().onErrorMap(RedisErrors::translate).toFuture();
        } catch (RuntimeException failure) {
            return CompletableFuture.failedFuture(RedisErrors.translate(failure));
        }
    }

    public <T> CompletionStage<Void> setAll(Map<String, T> entries) {
        return setAll(namespace, entries);
    }

    public <T> CompletionStage<Void> setAll(String namespace, Map<String, T> entries) {
        return run(
                () -> {
                    var encoded = namespace == null ? batches.encode(entries) : batches.encode(namespace, entries);
                    if (encoded.isEmpty()) return Mono.empty();
                    return redis.opsForValue().multiSet(encoded).then();
                });
    }

    public <T> CompletionStage<Map<String, T>> getAll(Collection<String> keys, Class<T> type) {
        return getAll(namespace, keys, type);
    }

    public <T> CompletionStage<Map<String, T>> getAll(String namespace, Collection<String> keys, Class<T> type) {
        return run(
                () -> {
                    if (type == null) throw RedisErrors.invalid();
                    return readAll(namespace, keys, json -> codec.decode(json, type));
                });
    }

    public <T> CompletionStage<Map<String, T>> getAll(
            Collection<String> keys, TypeReference<T> type) {
        return getAll(namespace, keys, type);
    }

    public <T> CompletionStage<Map<String, T>> getAll(
            String namespace, Collection<String> keys, TypeReference<T> type) {
        return run(
                () -> {
                    if (type == null) throw RedisErrors.invalid();
                    return readAll(namespace, keys, json -> codec.decode(json, type));
                });
    }

    private <T> Mono<Map<String, T>> readAll(String namespace, Collection<String> keys, Function<String, T> decoder) {
        var prepared = batches.keys(namespace, keys);
        if (prepared.isEmpty()) return Mono.just(Map.of());
        return redis.opsForValue()
                .multiGet(new ArrayList<>(prepared.values()))
                .map(values -> batches.decode(prepared, values, decoder))
                .switchIfEmpty(Mono.error(RedisErrors.error("unavailable", null)));
    }

    public <T> CompletionStage<Void> set(String key, T value) {
        return run(() -> redis.opsForValue().set(physicalKey(key, namespace), codec.encode(value)).then());
    }

    public <T> CompletionStage<Void> set(String namespace, String key, T value) {
        return run(() -> redis.opsForValue().set(physicalKey(key, namespace), codec.encode(value)).then());
    }

    public <T> CompletionStage<Void> set(String key, T value, Duration ttl) {
        return run(() -> redis.opsForValue().set(physicalKey(key, namespace), codec.encode(value), access.duration(ttl)).then());
    }

    public <T> CompletionStage<Void> setWithTtl(String namespace, String key, T value, Duration ttl) {
        return run(
                () ->
                        redis.opsForValue()
                                .set(physicalKey(key, namespace), codec.encode(value), access.duration(ttl))
                                .then());
    }

    public <T> CompletionStage<Optional<T>> get(String key, Class<T> type) {
        return getValue(key, type, namespace);
    }

    public <T> CompletionStage<Optional<T>> get(String namespace, String key, Class<T> type) {
        return getValue(key, type, namespace);
    }

    private <T> CompletionStage<Optional<T>> getValue(String key, Class<T> type, String namespace) {
        return run(
                () -> {
                    if (type == null) throw RedisErrors.invalid();
                    return redis.execute(
                                    RedisReadScripts.GET,
                                    List.of(physicalKey(key, namespace)),
                                    List.of(
                                            Integer.toString(
                                                    access.properties().maxPayloadBytes())))
                            .next()
                            .map(json -> Optional.of(codec.decode(json, type)))
                            .defaultIfEmpty(Optional.empty());
                });
    }

    public <T> CompletionStage<Optional<T>> get(String key, TypeReference<T> type) {
        return getValue(key, type, namespace);
    }

    public <T> CompletionStage<Optional<T>> get(String namespace, String key, TypeReference<T> type) {
        return getValue(key, type, namespace);
    }

    private <T> CompletionStage<Optional<T>> getValue(String key, TypeReference<T> type, String namespace) {
        return run(
                () -> {
                    if (type == null) throw RedisErrors.invalid();
                    return redis.execute(
                                    RedisReadScripts.GET,
                                    List.of(physicalKey(key, namespace)),
                                    List.of(
                                            Integer.toString(
                                                    access.properties().maxPayloadBytes())))
                            .next()
                            .map(json -> Optional.of(codec.decode(json, type)))
                            .defaultIfEmpty(Optional.empty());
                });
    }

    public CompletionStage<Boolean> exists(String key) {
        return run(() -> redis.hasKey(physicalKey(key, namespace)));
    }

    public CompletionStage<Boolean> exists(String namespace, String key) {
        return run(() -> redis.hasKey(physicalKey(key, namespace)));
    }

    public CompletionStage<Boolean> delete(String key) {
        return run(() -> redis.delete(physicalKey(key, namespace)).map(count -> count > 0));
    }

    public CompletionStage<Boolean> delete(String namespace, String key) {
        return run(() -> redis.delete(physicalKey(key, namespace)).map(count -> count > 0));
    }

    public CompletionStage<Optional<Duration>> ttl(String key) {
        return ttlWithNamespace(key, namespace);
    }

    public CompletionStage<Optional<Duration>> ttl(String namespace, String key) {
        return ttlWithNamespace(key, namespace);
    }

    private CompletionStage<Optional<Duration>> ttlWithNamespace(String key, String namespace) {
        return run(
                () -> {
                    String physicalKey = physicalKey(key, namespace);
                    // getExpire collapses persistent (-1) into Duration.ZERO; use raw reactive
                    // PTTL.
                    return redis.execute(
                                    connection ->
                                            connection
                                                    .keyCommands()
                                                    .pTtl(
                                                            java.nio.ByteBuffer.wrap(
                                                                    physicalKey.getBytes(
                                                                            java.nio.charset
                                                                                    .StandardCharsets
                                                                                    .UTF_8))))
                            .next()
                            .map(
                                    millis ->
                                            millis < 0
                                                    ? Optional.<Duration>empty()
                                                    : Optional.of(Duration.ofMillis(millis)))
                            .defaultIfEmpty(Optional.empty());
                });
    }

    public CompletionStage<Boolean> expire(String key, Duration ttl) {
        return run(() -> redis.expire(physicalKey(key, namespace), access.duration(ttl)));
    }

    public CompletionStage<Boolean> expire(String namespace, String key, Duration ttl) {
        return run(() -> redis.expire(physicalKey(key, namespace), access.duration(ttl)));
    }

    public CompletionStage<Boolean> persist(String key) {
        return run(() -> redis.persist(physicalKey(key, namespace)));
    }

    public CompletionStage<Boolean> persist(String namespace, String key) {
        return run(() -> redis.persist(physicalKey(key, namespace)));
    }

    public <T> CompletionStage<Boolean> setIfAbsent(String key, T value) {
        return run(() -> redis.opsForValue().setIfAbsent(physicalKey(key, namespace), codec.encode(value)));
    }

    public <T> CompletionStage<Boolean> setIfAbsent(String namespace, String key, T value) {
        return run(() -> redis.opsForValue().setIfAbsent(physicalKey(key, namespace), codec.encode(value)));
    }

    public <T> CompletionStage<Boolean> setIfAbsent(String key, T value, Duration ttl) {
        return run(() -> redis.opsForValue().setIfAbsent(physicalKey(key, namespace), codec.encode(value), access.duration(ttl)));
    }

    public <T> CompletionStage<Boolean> setIfAbsentWithTtl(String namespace, String key, T value, Duration ttl) {
        return run(
                () ->
                        redis.opsForValue()
                                .setIfAbsent(
                                        physicalKey(key, namespace),
                                        codec.encode(value),
                                        access.duration(ttl)));
    }

    private String physicalKey(String key, String namespace) {
        return namespace == null ? access.key(key) : access.key(namespace, key);
    }
}
