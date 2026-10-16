package br.com.portalmanager.platform.library.redis.operations;

/** Native Redis operations; bounded reads and JSON values, no implicit type conversion. */
public interface RedisOperations {
    RedisKeys keys();

    RedisHashes hashes();

    RedisSets sets();

    RedisLists lists();

    RedisSortedSets sortedSets();

    RedisAtomic atomic();
}
