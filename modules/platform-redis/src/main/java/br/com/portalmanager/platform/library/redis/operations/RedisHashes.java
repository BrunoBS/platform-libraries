package br.com.portalmanager.platform.library.redis.operations;

import java.util.Map;
import java.util.Optional;

/** Native Redis operations; bounded reads and JSON values, no implicit type conversion. */
public interface RedisHashes {
    <T> void put(String key, String field, T value);

    <T> Optional<T> get(String key, String field, Class<T> type);

    <T> Map<String, T> getAll(String key, Class<T> type, int limit);

    boolean remove(String key, String field);

    boolean exists(String key, String field);
}
