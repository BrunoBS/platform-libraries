package br.com.portalmanager.platform.library.messaging.config;

import br.com.portalmanager.platform.library.messaging.cache.ApiMessageCache;
import br.com.portalmanager.platform.library.messaging.cache.RedisApiMessageCache;
import br.com.portalmanager.platform.library.messaging.exception.PlatformConfigurationException;
import br.com.portalmanager.platform.library.messaging.message.PlatformTechnicalErrors;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.data.redis.core.StringRedisTemplate;

import java.time.Duration;

@AutoConfiguration(before = PlatformMessagingAutoConfiguration.class)
@ConditionalOnClass(StringRedisTemplate.class)
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
            StringRedisTemplate redisTemplate
    ) {
        Duration ttl = properties.getCache().getTtl();
        if (ttl == null || ttl.isZero() || ttl.isNegative()) {
            throw new PlatformConfigurationException(PlatformTechnicalErrors.CACHE_TTL_INVALID);
        }
        return new RedisApiMessageCache(redisTemplate, ttl);
    }
}
