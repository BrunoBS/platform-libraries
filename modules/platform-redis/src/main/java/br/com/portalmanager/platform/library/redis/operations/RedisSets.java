package br.com.portalmanager.platform.library.redis.operations;

import java.util.Set;

/** Native Redis operations; bounded reads and JSON values, no implicit type conversion. */
public interface RedisSets {
    <T> boolean add(String key, T value);

    <T> boolean remove(String key, T value);

    <T> boolean contains(String key, T value);

    <T> Set<T> members(String key, Class<T> type, int limit);

    long count(String key);
}
