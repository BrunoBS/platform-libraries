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

    public DefaultAsyncRedisStore(
            ReactiveStringRedisTemplate redis, RedisJsonCodec codec, RedisAccess access) {
        this.redis = redis;
        this.codec = codec;
        this.access = access;
        this.batches = new RedisBatches(access, codec, redis.getConnectionFactory());
    }

    private <T> CompletionStage<T> run(Supplier<Mono<T>> operation) {
        try {
            return operation.get().onErrorMap(RedisErrors::translate).toFuture();
        } catch (RuntimeException failure) {
            return CompletableFuture.failedFuture(RedisErrors.translate(failure));
        }
    }

    public <T> CompletionStage<Void> setAll(Map<String, T> entries) {
        return run(
                () -> {
                    var encoded = batches.encode(entries);
                    if (encoded.isEmpty()) return Mono.empty();
                    return redis.opsForValue().multiSet(encoded).then();
                });
    }

    public <T> CompletionStage<Map<String, T>> getAll(Collection<String> keys, Class<T> type) {
        return run(
                () -> {
                    if (type == null) throw RedisErrors.invalid();
                    return readAll(keys, json -> codec.decode(json, type));
                });
    }

    public <T> CompletionStage<Map<String, T>> getAll(
            Collection<String> keys, TypeReference<T> type) {
        return run(
                () -> {
                    if (type == null) throw RedisErrors.invalid();
                    return readAll(keys, json -> codec.decode(json, type));
                });
    }

    private <T> Mono<Map<String, T>> readAll(Collection<String> keys, Function<String, T> decoder) {
        var prepared = batches.keys(keys);
        if (prepared.isEmpty()) return Mono.just(Map.of());
        return redis.opsForValue()
                .multiGet(new ArrayList<>(prepared.values()))
                .map(values -> batches.decode(prepared, values, decoder))
                .switchIfEmpty(Mono.error(RedisErrors.error("unavailable", null)));
    }

    public <T> CompletionStage<Void> set(String key, T value) {
        return run(() -> redis.opsForValue().set(access.key(key), codec.encode(value)).then());
    }

    public <T> CompletionStage<Void> set(String key, T value, Duration ttl) {
        return run(
                () ->
                        redis.opsForValue()
                                .set(access.key(key), codec.encode(value), access.duration(ttl))
                                .then());
    }

    public <T> CompletionStage<Optional<T>> get(String key, Class<T> type) {
        return run(
                () -> {
                    if (type == null) throw RedisErrors.invalid();
                    return redis.execute(
                                    RedisReadScripts.GET,
                                    List.of(access.key(key)),
                                    List.of(
                                            Integer.toString(
                                                    access.properties().maxPayloadBytes())))
                            .next()
                            .map(json -> Optional.of(codec.decode(json, type)))
                            .defaultIfEmpty(Optional.empty());
                });
    }

    public <T> CompletionStage<Optional<T>> get(String key, TypeReference<T> type) {
        return run(
                () -> {
                    if (type == null) throw RedisErrors.invalid();
                    return redis.execute(
                                    RedisReadScripts.GET,
                                    List.of(access.key(key)),
                                    List.of(
                                            Integer.toString(
                                                    access.properties().maxPayloadBytes())))
                            .next()
                            .map(json -> Optional.of(codec.decode(json, type)))
                            .defaultIfEmpty(Optional.empty());
                });
    }

    public CompletionStage<Boolean> exists(String key) {
        return run(() -> redis.hasKey(access.key(key)));
    }

    public CompletionStage<Boolean> delete(String key) {
        return run(() -> redis.delete(access.key(key)).map(count -> count > 0));
    }

    public CompletionStage<Optional<Duration>> ttl(String key) {
        return run(
                () -> {
                    String physicalKey = access.key(key);
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
        return run(() -> redis.expire(access.key(key), access.duration(ttl)));
    }

    public CompletionStage<Boolean> persist(String key) {
        return run(() -> redis.persist(access.key(key)));
    }

    public <T> CompletionStage<Boolean> setIfAbsent(String key, T value) {
        return run(() -> redis.opsForValue().setIfAbsent(access.key(key), codec.encode(value)));
    }

    public <T> CompletionStage<Boolean> setIfAbsent(String key, T value, Duration ttl) {
        return run(
                () ->
                        redis.opsForValue()
                                .setIfAbsent(
                                        access.key(key),
                                        codec.encode(value),
                                        access.duration(ttl)));
    }
}
