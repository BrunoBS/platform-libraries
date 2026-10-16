package br.com.portalmanager.platform.library.redis.core;

import tools.jackson.core.type.TypeReference;

import java.time.Duration;
import java.util.Collection;
import java.util.Map;
import java.util.Optional;

/** JSON String storage. ttl empty means absent OR persistent. */
public interface RedisStore {
    /** One MSET, persistent values. Cluster batches require a common hash slot. */
    <T> void setAll(Map<String, T> entries);

    /** One MGET; missing entries omitted, first requested occurrence order preserved. */
    <T> Map<String, T> getAll(Collection<String> keys, Class<T> type);

    <T> Map<String, T> getAll(Collection<String> keys, TypeReference<T> type);

    <T> void set(String key, T value);

    <T> void set(String key, T value, Duration ttl);

    <T> Optional<T> get(String key, Class<T> type);

    <T> Optional<T> get(String key, TypeReference<T> type);

    boolean exists(String key);

    boolean delete(String key);

    Optional<Duration> ttl(String key);

    boolean expire(String key, Duration ttl);

    boolean persist(String key);

    <T> boolean setIfAbsent(String key, T value);

    <T> boolean setIfAbsent(String key, T value, Duration ttl);

    <T> RedisWrite<T> write(String key, T value);
}
