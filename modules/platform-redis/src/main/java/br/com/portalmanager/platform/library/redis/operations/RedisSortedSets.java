package br.com.portalmanager.platform.library.redis.operations;

import java.util.Optional;
import java.util.Set;

/** Native Redis operations; bounded reads and JSON values, no implicit type conversion. */
public interface RedisSortedSets {
    <T> boolean add(String key, T value, double score);

    <T> boolean remove(String key, T value);

    <T> Optional<Double> score(String key, T value);

    <T> double incrementScore(String key, T value, double delta);

    <T> Set<T> range(String key, long start, long end, Class<T> type);

    <T> Optional<Long> rank(String key, T value);

    long count(String key);
}
