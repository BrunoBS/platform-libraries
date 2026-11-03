package br.com.portalmanager.platform.library.messaging.config;

import br.com.portalmanager.platform.library.messaging.cache.ApiMessageCache;
import br.com.portalmanager.platform.library.messaging.cache.RedisApiMessageCache;
import br.com.portalmanager.platform.library.messaging.exception.PlatformConfigurationException;
import br.com.portalmanager.platform.library.messaging.message.PlatformTechnicalErrors;
import br.com.portalmanager.platform.library.redis.configuration.PlatformRedisAutoConfiguration;
import br.com.portalmanager.platform.library.redis.store.RedisStore;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;

import java.time.Duration;

@AutoConfiguration(
        after = PlatformRedisAutoConfiguration.class,
        before = PlatformMessagingAutoConfiguration.class
)
@ConditionalOnBean(RedisStore.class)
@ConditionalOnProperty(
        prefix = "platform.messaging.cache",
        name = "enabled",
        havingValue = "true"
)
public class PlatformMessagingRedisAutoConfiguration {

    @Bean
    @ConditionalOnMissingBean(ApiMessageCache.class)
    ApiMessageCache redisApiMessageCache(
            PlatformMessagingProperties properties,
            RedisStore redisStore
    ) {
        Duration ttl = properties.getCache().getTtl();
        if (ttl == null || ttl.isZero() || ttl.isNegative()) {
            throw new PlatformConfigurationException(PlatformTechnicalErrors.CACHE_TTL_INVALID);
        }
        return new RedisApiMessageCache(redisStore, ttl);
    }
}
