package br.com.portalmanager.platform.library.redis.internal.operations;

import br.com.portalmanager.platform.library.redis.internal.common.RedisAccess;
import br.com.portalmanager.platform.library.redis.internal.serialization.RedisJsonCodec;
import br.com.portalmanager.platform.library.redis.api.operations.RedisAtomic;
import br.com.portalmanager.platform.library.redis.api.operations.RedisHashes;
import br.com.portalmanager.platform.library.redis.api.operations.RedisKeys;
import br.com.portalmanager.platform.library.redis.api.operations.RedisLists;
import br.com.portalmanager.platform.library.redis.api.operations.RedisOperations;
import br.com.portalmanager.platform.library.redis.api.operations.RedisSets;
import br.com.portalmanager.platform.library.redis.api.operations.RedisSortedSets;
import br.com.portalmanager.platform.library.redis.api.store.RedisStore;
import org.springframework.data.redis.core.StringRedisTemplate;

/** Facade that composes the internal implementation for each Redis operation family. */
public final class DefaultRedisOperations implements RedisOperations {
    private final RedisKeys keys;
    private final RedisHashes hashes;
    private final RedisSets sets;
    private final RedisLists lists;
    private final RedisSortedSets sortedSets;
    private final RedisAtomic atomic;

    public DefaultRedisOperations(
            StringRedisTemplate redis, RedisJsonCodec codec, RedisAccess access, RedisStore store) {
        var context = new RedisOperationContext(redis, codec, access, store);
        this.keys = new DefaultRedisKeys(context);
        this.hashes = new DefaultRedisHashes(context);
        this.sets = new DefaultRedisSets(context);
        this.lists = new DefaultRedisLists(context);
        this.sortedSets = new DefaultRedisSortedSets(context);
        this.atomic = new DefaultRedisAtomic(context);
    }

    public RedisKeys keys() {
        return keys;
    }

    public RedisHashes hashes() {
        return hashes;
    }

    public RedisSets sets() {
        return sets;
    }

    public RedisLists lists() {
        return lists;
    }

    public RedisSortedSets sortedSets() {
        return sortedSets;
    }

    public RedisAtomic atomic() {
        return atomic;
    }
}
