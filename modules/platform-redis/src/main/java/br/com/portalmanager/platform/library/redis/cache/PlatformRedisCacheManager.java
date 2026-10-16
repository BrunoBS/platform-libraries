package br.com.portalmanager.platform.library.redis.cache;

import br.com.portalmanager.platform.library.redis.internal.RedisAccess;
import br.com.portalmanager.platform.library.redis.serialization.RedisJsonCodec;

import org.springframework.cache.Cache;
import org.springframework.cache.transaction.TransactionAwareCacheDecorator;
import org.springframework.data.redis.cache.RedisCache;
import org.springframework.data.redis.cache.RedisCacheConfiguration;
import org.springframework.data.redis.cache.RedisCacheManager;
import org.springframework.data.redis.cache.RedisCacheWriter;
import org.springframework.data.redis.serializer.RedisSerializationContext;
import org.springframework.data.redis.serializer.RedisSerializer;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

/** Extends the native manager solely for region-specific typed JSON and read policy views. */
public final class PlatformRedisCacheManager extends RedisCacheManager {
    private final PlatformRedisCacheProperties properties;
    private final CacheReturnTypes types;
    private final RedisJsonCodec codec;
    private final RedisAccess access;
    private final String service, environment;
    private final ConcurrentMap<String, Cache> reads = new ConcurrentHashMap<>();

    PlatformRedisCacheManager(
            RedisCacheWriter writer,
            PlatformRedisCacheProperties properties,
            CacheReturnTypes types,
            RedisJsonCodec codec,
            RedisAccess access,
            String service,
            String environment) {
        super(writer, RedisCacheConfiguration.defaultCacheConfig().disableCachingNullValues());
        this.properties = properties;
        this.types = types;
        this.codec = codec;
        this.access = access;
        this.service = service;
        this.environment = environment;
        access.text(service);
        PlatformRedisCacheProperties.name(environment);
        setTransactionAware(true);
    }

    protected RedisCache createRedisCache(String name, RedisCacheConfiguration ignored) {
        return new PolicyRedisCache(name, getCacheWriter(), configuration(name), false);
    }

    private RedisCacheConfiguration configuration(String name) {
        PlatformRedisCacheProperties.name(name);
        String prefix = access.key("cache:" + service + ":" + environment + ":" + name) + "::";
        RedisSerializer<String> keys =
                new RedisSerializer<>() {
                    public byte[] serialize(String value) {
                        return RedisSerializer.string().serialize(access.text(value));
                    }

                    public String deserialize(byte[] value) {
                        return RedisSerializer.string().deserialize(value);
                    }
                };
        return RedisCacheConfiguration.defaultCacheConfig()
                .disableCachingNullValues()
                .entryTtl(properties.ttl(name))
                .computePrefixWith(region -> prefix)
                .serializeKeysWith(RedisSerializationContext.SerializationPair.fromSerializer(keys))
                .serializeValuesWith(
                        RedisSerializationContext.SerializationPair.fromSerializer(
                                new CacheJsonSerializer(name, types, codec)));
    }

    Cache readCache(String name) {
        if (properties.failurePolicy() == PlatformRedisCacheProperties.FailurePolicy.FAIL_FAST)
            return getCache(name);
        getCache(name);
        return reads.computeIfAbsent(
                name,
                region ->
                        new TransactionAwareCacheDecorator(
                                new PolicyRedisCache(
                                        region, getCacheWriter(), configuration(region), true)));
    }
}
