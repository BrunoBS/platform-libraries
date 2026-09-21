package br.com.portalmanager.platform.messaging.autoconfigure;

import br.com.portalmanager.platform.messaging.cache.ApiMessageCache;
import br.com.portalmanager.platform.messaging.cache.RedisApiMessageCache;
import br.com.portalmanager.platform.messaging.config.PlatformMessagingProperties;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.data.redis.core.StringRedisTemplate;

import java.time.Duration;

@AutoConfiguration(after = PlatformMessagingAutoConfiguration.class)
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
            throw new IllegalArgumentException("Cache TTL must be positive");
        }
        return new RedisApiMessageCache(redisTemplate, ttl);
    }
}
