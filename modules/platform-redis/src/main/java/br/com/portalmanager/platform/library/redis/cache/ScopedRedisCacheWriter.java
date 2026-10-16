package br.com.portalmanager.platform.library.redis.cache;

import org.springframework.data.redis.cache.CacheStatistics;
import org.springframework.data.redis.cache.CacheStatisticsCollector;
import org.springframework.data.redis.cache.RedisCacheWriter;
import org.springframework.data.redis.connection.RedisConnection;

import java.time.Duration;
import java.util.concurrent.CompletableFuture;
import java.util.function.Function;
import java.util.function.Supplier;

/**
 * Namespaces native writer bookkeeping/locks as well as entry keys; delegates every Redis command.
 */
final class ScopedRedisCacheWriter implements RedisCacheWriter {
    private final RedisCacheWriter delegate;
    private final String scope;

    ScopedRedisCacheWriter(RedisCacheWriter delegate, String scope) {
        this.delegate = delegate;
        this.scope = scope;
    }

    private String name(String region) {
        PlatformRedisCacheProperties.name(region);
        return scope + region;
    }

    public byte[] get(String name, byte[] key) {
        return delegate.get(name(name), key);
    }

    public byte[] get(String name, byte[] key, Duration ttl) {
        return delegate.get(name(name), key, ttl);
    }

    public byte[] get(String name, byte[] key, Supplier<byte[]> loader, Duration ttl, boolean tti) {
        return delegate.get(name(name), key, loader, ttl, tti);
    }

    public boolean supportsAsyncRetrieve() {
        return delegate.supportsAsyncRetrieve();
    }

    public CompletableFuture<byte[]> retrieve(String name, byte[] key, Duration ttl) {
        return delegate.retrieve(name(name), key, ttl);
    }

    public void put(String name, byte[] key, byte[] value, Duration ttl) {
        delegate.put(name(name), key, value, ttl);
    }

    public CompletableFuture<Void> store(String name, byte[] key, byte[] value, Duration ttl) {
        return delegate.store(name(name), key, value, ttl);
    }

    public byte[] putIfAbsent(String name, byte[] key, byte[] value, Duration ttl) {
        return delegate.putIfAbsent(name(name), key, value, ttl);
    }

    public void evict(String name, byte[] key) {
        delegate.evict(name(name), key);
    }

    public boolean evictIfPresent(String name, byte[] key) {
        return delegate.evictIfPresent(name(name), key);
    }

    public void clear(String name, byte[] pattern) {
        delegate.clear(name(name), pattern);
    }

    public boolean invalidate(String name, byte[] pattern) {
        return delegate.invalidate(name(name), pattern);
    }

    public void clearStatistics(String name) {
        delegate.clearStatistics(name(name));
    }

    public CacheStatistics getCacheStatistics(String name) {
        return delegate.getCacheStatistics(name(name));
    }

    public RedisCacheWriter withStatisticsCollector(CacheStatisticsCollector collector) {
        return new ScopedRedisCacheWriter(delegate.withStatisticsCollector(collector), scope);
    }

    public <T> T execute(Function<RedisConnection, T> action) {
        return delegate.execute(action);
    }
}
