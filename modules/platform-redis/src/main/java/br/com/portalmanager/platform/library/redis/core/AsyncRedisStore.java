package br.com.portalmanager.platform.library.redis.core;

import tools.jackson.core.type.TypeReference;

import java.time.Duration;
import java.util.Collection;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.CompletionStage;

/** Uses driver reactive I/O; serialization executes on the calling/completion thread. */
public interface AsyncRedisStore {
    /** One MSET, persistent values. Cluster batches require a common hash slot. */
    <T> CompletionStage<Void> setAll(Map<String, T> entries);

    /** One MGET; missing entries omitted, first requested occurrence order preserved. */
    <T> CompletionStage<Map<String, T>> getAll(Collection<String> keys, Class<T> type);

    <T> CompletionStage<Map<String, T>> getAll(Collection<String> keys, TypeReference<T> type);

    <T> CompletionStage<Void> set(String key, T value);

    <T> CompletionStage<Void> set(String key, T value, Duration ttl);

    <T> CompletionStage<Optional<T>> get(String key, Class<T> type);

    <T> CompletionStage<Optional<T>> get(String key, TypeReference<T> type);

    CompletionStage<Boolean> exists(String key);

    CompletionStage<Boolean> delete(String key);

    CompletionStage<Optional<Duration>> ttl(String key);

    CompletionStage<Boolean> expire(String key, Duration ttl);

    CompletionStage<Boolean> persist(String key);

    <T> CompletionStage<Boolean> setIfAbsent(String key, T value);

    <T> CompletionStage<Boolean> setIfAbsent(String key, T value, Duration ttl);
}
