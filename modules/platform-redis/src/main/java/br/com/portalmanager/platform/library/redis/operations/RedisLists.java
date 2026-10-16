package br.com.portalmanager.platform.library.redis.operations;

import java.util.List;
import java.util.Optional;

/** Native Redis operations; bounded reads and JSON values, no implicit type conversion. */
public interface RedisLists {
    <T> long pushLeft(String key, T value);

    <T> long pushRight(String key, T value);

    <T> Optional<T> popLeft(String key, Class<T> type);

    <T> Optional<T> popRight(String key, Class<T> type);

    <T> List<T> range(String key, long start, long end, Class<T> type);

    long length(String key);

    void trim(String key, long start, long end);
}
