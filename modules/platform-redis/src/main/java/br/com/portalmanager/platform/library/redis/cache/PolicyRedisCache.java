package br.com.portalmanager.platform.library.redis.cache;

import br.com.portalmanager.platform.library.messaging.exception.ApiException;
import br.com.portalmanager.platform.library.redis.internal.RedisErrors;

import org.slf4j.LoggerFactory;
import org.springframework.data.redis.cache.RedisCache;
import org.springframework.data.redis.cache.RedisCacheConfiguration;
import org.springframework.data.redis.cache.RedisCacheWriter;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.util.concurrent.Callable;

/** Small policy extension: native Spring Data Redis still performs storage, loading and locking. */
final class PolicyRedisCache extends RedisCache {
    private final boolean failOpenRead;

    /**
     * Internal control flow: abort the native loader before SET, while its finally block unlocks.
     */
    private static final class NullLoaderResult extends RuntimeException {
        private NullLoaderResult() {
            super(null, null, false, false);
        }
    }

    protected <T> T loadCacheValue(Object key, Callable<T> loader) {
        T value = super.loadCacheValue(key, loader);
        if (value == null) throw new NullLoaderResult();
        return value;
    }

    PolicyRedisCache(
            String name,
            RedisCacheWriter writer,
            RedisCacheConfiguration config,
            boolean failOpenRead) {
        super(name, writer, config);
        this.failOpenRead = failOpenRead;
    }

    private boolean ignore(RuntimeException failure) {
        ApiException error = RedisErrors.translate(failure);
        boolean ignored =
                failOpenRead
                        && (error.getMessageKey().equals("redis.unavailable")
                                || error.getMessageKey().equals("redis.timeout")
                                || error.getMessageKey().equals("redis.authentication"));
        if (ignored)
            LoggerFactory.getLogger(PolicyRedisCache.class)
                    .warn("Redis read cache bypassed code={}", error.getMessageKey());
        return ignored;
    }

    protected Object lookup(Object key) {
        try {
            return super.lookup(key);
        } catch (RuntimeException failure) {
            if (ignore(failure)) return null;
            throw RedisErrors.translate(failure);
        }
    }

    public void put(Object key, Object value) {
        if (value == null) return;
        try {
            super.put(key, value);
        } catch (RuntimeException failure) {
            if (!ignore(failure)) throw RedisErrors.translate(failure);
        }
    }

    public ValueWrapper putIfAbsent(Object key, Object value) {
        try {
            return super.putIfAbsent(key, value);
        } catch (RuntimeException failure) {
            throw RedisErrors.translate(failure);
        }
    }

    public void evict(Object key) {
        try {
            super.evict(key);
        } catch (RuntimeException failure) {
            throw RedisErrors.translate(failure);
        }
    }

    public boolean evictIfPresent(Object key) {
        try {
            return super.evictIfPresent(key);
        } catch (RuntimeException failure) {
            throw RedisErrors.translate(failure);
        }
    }

    public void clear() {
        try {
            super.clear();
        } catch (RuntimeException failure) {
            throw RedisErrors.translate(failure);
        }
    }

    public boolean invalidate() {
        try {
            return super.invalidate();
        } catch (RuntimeException failure) {
            throw RedisErrors.translate(failure);
        }
    }

    protected Object toStoreValue(Object value) {
        return value == null ? null : super.toStoreValue(value);
    }

    public <T> T get(Object key, Callable<T> loader) {
        if (TransactionSynchronizationManager.isActualTransactionActive())
            throw CacheErrors.syncTransaction();
        try {
            return super.get(key, loader);
        } catch (NullLoaderResult absent) {
            return null;
        } catch (ValueRetrievalException methodFailure) {
            throw methodFailure;
        } catch (RuntimeException failure) {
            if (!ignore(failure)) throw RedisErrors.translate(failure);
            try {
                return loader.call();
            } catch (Exception methodFailure) {
                throw new ValueRetrievalException(key, loader, methodFailure);
            }
        }
    }
}
