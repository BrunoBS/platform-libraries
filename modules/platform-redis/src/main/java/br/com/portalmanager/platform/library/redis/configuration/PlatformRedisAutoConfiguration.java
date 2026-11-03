package br.com.portalmanager.platform.library.redis.configuration;

import br.com.portalmanager.platform.library.redis.core.AsyncRedisStore;
import br.com.portalmanager.platform.library.redis.core.RedisListenerErrorHandler;
import br.com.portalmanager.platform.library.redis.core.RedisListenerMetrics;
import br.com.portalmanager.platform.library.redis.core.RedisStore;
import br.com.portalmanager.platform.library.redis.internal.DefaultAsyncRedisStore;
import br.com.portalmanager.platform.library.redis.internal.DefaultRedisOperations;
import br.com.portalmanager.platform.library.redis.internal.DefaultRedisPublisher;
import br.com.portalmanager.platform.library.redis.internal.DefaultRedisStore;
import br.com.portalmanager.platform.library.redis.internal.DefaultRedisStreams;
import br.com.portalmanager.platform.library.redis.internal.ListenerSupport;
import br.com.portalmanager.platform.library.redis.internal.RedisAccess;
import br.com.portalmanager.platform.library.redis.internal.RedisPubSubLifecycle;
import br.com.portalmanager.platform.library.redis.internal.RedisStreamsLifecycle;
import br.com.portalmanager.platform.library.redis.operations.RedisOperations;
import br.com.portalmanager.platform.library.redis.pubsub.RedisPublisher;
import br.com.portalmanager.platform.library.redis.serialization.RedisJsonCodec;
import br.com.portalmanager.platform.library.redis.stream.RedisStreams;

import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.core.env.Environment;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.core.ReactiveStringRedisTemplate;
import org.springframework.data.redis.core.StringRedisTemplate;

import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.json.JsonMapper;

@AutoConfiguration(
        afterName = {
            "org.springframework.boot.data.redis.autoconfigure.DataRedisAutoConfiguration",
            "org.springframework.boot.data.redis.autoconfigure.DataRedisReactiveAutoConfiguration",
            "org.springframework.boot.jackson.autoconfigure.JacksonAutoConfiguration"
        })
@EnableConfigurationProperties(PlatformRedisProperties.class)
@ConditionalOnClass(StringRedisTemplate.class)
@ConditionalOnProperty(
        prefix = "platform.redis",
        name = "enabled",
        havingValue = "true",
        matchIfMissing = true)
public class PlatformRedisAutoConfiguration {
    @Bean
    @ConditionalOnMissingBean
    RedisAccess redisAccess(PlatformRedisProperties properties, Environment environment) {
        return new RedisAccess(properties, environment.getProperty("spring.application.name"));
    }

    @Bean
    @ConditionalOnMissingBean
    RedisJsonCodec redisJsonCodec(
            ObjectProvider<ObjectMapper> mapper, PlatformRedisProperties properties) {
        return new RedisJsonCodec(
                mapper.getIfAvailable(JsonMapper::new), properties.maxPayloadBytes());
    }

    @Bean
    @ConditionalOnMissingBean(RedisStore.class)
    RedisStore redisStore(StringRedisTemplate redis, RedisJsonCodec codec, RedisAccess access) {
        return new DefaultRedisStore(redis, codec, access);
    }

    @Bean
    @ConditionalOnMissingBean(AsyncRedisStore.class)
    AsyncRedisStore asyncRedisStore(
            ReactiveStringRedisTemplate redis, RedisJsonCodec codec, RedisAccess access) {
        return new DefaultAsyncRedisStore(redis, codec, access);
    }

    @Bean
    @ConditionalOnMissingBean(RedisOperations.class)
    RedisOperations redisOperations(
            StringRedisTemplate redis, RedisJsonCodec codec, RedisAccess access, RedisStore store) {
        return new DefaultRedisOperations(redis, codec, access, store);
    }

    @Bean
    @ConditionalOnMissingBean(RedisPublisher.class)
    RedisPublisher redisPublisher(
            StringRedisTemplate redis, RedisJsonCodec codec, RedisAccess access) {
        return new DefaultRedisPublisher(redis, codec, access);
    }

    @Bean
    @ConditionalOnMissingBean
    RedisListenerMetrics redisListenerMetrics() {
        return new RedisListenerMetrics();
    }

    @Bean
    @ConditionalOnMissingBean
    RedisListenerErrorHandler redisListenerErrorHandler() {
        return (transport, failure) ->
                LoggerFactory.getLogger(PlatformRedisAutoConfiguration.class)
                        .error(
                                "Platform Redis listener failure transport={} code={}",
                                transport,
                                failure.getMessageKey());
    }

    @Bean
    @ConditionalOnMissingBean
    ListenerSupport redisListenerSupport(
            RedisListenerErrorHandler errors,
            RedisListenerMetrics metrics,
            PlatformRedisProperties properties) {
        return new ListenerSupport(errors, metrics, properties);
    }

    @Bean
    @ConditionalOnMissingBean
    RedisPubSubLifecycle redisPubSubLifecycle(
            ConfigurableListableBeanFactory beans,
            RedisConnectionFactory connections,
            RedisJsonCodec codec,
            RedisAccess access,
            ListenerSupport support) {
        return new RedisPubSubLifecycle(beans, connections, codec, access, support);
    }

    @Bean
    @ConditionalOnMissingBean(RedisStreams.class)
    DefaultRedisStreams redisStreams(
            StringRedisTemplate redis, RedisJsonCodec codec, RedisAccess access) {
        return new DefaultRedisStreams(redis, codec, access);
    }

    @Bean
    @ConditionalOnMissingBean
    RedisStreamsLifecycle redisStreamsLifecycle(
            ConfigurableListableBeanFactory beans,
            StringRedisTemplate redis,
            RedisJsonCodec codec,
            RedisAccess access,
            ListenerSupport support) {
        return new RedisStreamsLifecycle(
                beans, new DefaultRedisStreams(redis, codec, access), codec, access, support);
    }
}
