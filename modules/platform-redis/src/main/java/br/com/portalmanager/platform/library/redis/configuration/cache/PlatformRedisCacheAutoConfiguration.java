package br.com.portalmanager.platform.library.redis.configuration.cache;

import br.com.portalmanager.platform.library.redis.api.configuration.PlatformRedisCacheProperties;
import br.com.portalmanager.platform.library.redis.configuration.PlatformRedisAutoConfiguration;
import br.com.portalmanager.platform.library.redis.internal.cache.CacheReturnTypes;
import br.com.portalmanager.platform.library.redis.internal.cache.PlatformRedisCacheManager;
import br.com.portalmanager.platform.library.redis.internal.cache.ScopedRedisCacheWriter;
import br.com.portalmanager.platform.library.redis.internal.common.RedisAccess;
import br.com.portalmanager.platform.library.redis.internal.serialization.RedisJsonCodec;

import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.CachingConfigurer;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.cache.interceptor.CacheResolver;
import org.springframework.cache.interceptor.CacheableOperation;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.cache.BatchStrategies;
import org.springframework.data.redis.cache.RedisCacheWriter;
import org.springframework.data.redis.connection.RedisConnectionFactory;

import java.time.Duration;

@AutoConfiguration(
        after = PlatformRedisAutoConfiguration.class,
        afterName = "org.springframework.boot.transaction.autoconfigure.TransactionAutoConfiguration",
        beforeName = "org.springframework.boot.cache.autoconfigure.CacheAutoConfiguration")
@ConditionalOnProperty(prefix = "platform.redis.cache", name = "enabled", havingValue = "true")
@ConditionalOnProperty(
        prefix = "platform.redis",
        name = "enabled",
        havingValue = "true",
        matchIfMissing = true)
@EnableCaching
@EnableConfigurationProperties(PlatformRedisCacheProperties.class)
public class PlatformRedisCacheAutoConfiguration {
    @Configuration(proxyBeanMethods = false)
    @ConditionalOnMissingBean(CacheManager.class)
    static class ManagedCacheConfiguration {
        @Bean
        static CacheReturnTypes redisCacheReturnTypes() {
            return new CacheReturnTypes();
        }

        @Bean(name = "platformRedisCacheManager")
        PlatformRedisCacheManager platformRedisCacheManager(
                RedisConnectionFactory connection,
                PlatformRedisCacheProperties properties,
                CacheReturnTypes types,
                RedisJsonCodec codec,
                RedisAccess access) {
            var writer =
                    RedisCacheWriter.create(
                            connection,
                            config ->
                                    config.immediateWrites(true)
                                            .batchStrategy(
                                                    BatchStrategies.scan(
                                                            access.properties().limits().maxReadSize()))
                                            .enableLocking(
                                                    lock ->
                                                            lock.sleepTime(Duration.ofMillis(10))
                                                                    .lockTimeout(
                                                                            RedisCacheWriter
                                                                                    .TtlFunction
                                                                                    .just(
                                                                                            properties
                                                                                                    .lockTtl()))));
            var scopedWriter =
                    new ScopedRedisCacheWriter(
                            writer, access.key("cache:"));
            return new PlatformRedisCacheManager(
                    scopedWriter, properties, types, codec, access);
        }
    }

    @Bean(name = "platformRedisCacheResolver")
    @ConditionalOnMissingBean(name = "platformRedisCacheResolver")
    CacheResolver platformRedisCacheResolver(ObjectProvider<CacheManager> managers) {
        return context -> {
            CacheManager manager = managers.getIfUnique();
            if (manager == null)
                throw br.com.portalmanager.platform.library.redis.internal.common.RedisErrors
                        .configuration();
            return context.getOperation().getCacheNames().stream()
                    .map(
                            name ->
                                    manager instanceof PlatformRedisCacheManager platform
                                                    && context.getOperation()
                                                            instanceof CacheableOperation
                                            ? platform.readCache(name)
                                            : manager.getCache(name))
                    .toList();
        };
    }

    @Bean
    @ConditionalOnMissingBean(CachingConfigurer.class)
    CachingConfigurer platformRedisCachingConfigurer(
            @org.springframework.beans.factory.annotation.Qualifier("platformRedisCacheResolver")
                    CacheResolver resolver) {
        return new CachingConfigurer() {
            public CacheResolver cacheResolver() {
                return resolver;
            }
        };
    }
}
