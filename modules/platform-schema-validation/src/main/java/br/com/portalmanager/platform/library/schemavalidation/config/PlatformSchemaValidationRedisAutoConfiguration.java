package br.com.portalmanager.platform.library.schemavalidation.config;

import br.com.portalmanager.platform.library.schemavalidation.cache.RedisResourceSchemaCache;
import br.com.portalmanager.platform.library.schemavalidation.cache.ResourceSchemaCache;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.data.redis.core.StringRedisTemplate;
import tools.jackson.databind.ObjectMapper;

@AutoConfiguration(before = PlatformSchemaValidationAutoConfiguration.class)
@ConditionalOnClass(StringRedisTemplate.class)
@ConditionalOnProperty(prefix = "platform.schema-validation.cache.redis", name = "enabled", havingValue = "true")
public class PlatformSchemaValidationRedisAutoConfiguration {

    @Bean
    @ConditionalOnBean(StringRedisTemplate.class)
    @ConditionalOnMissingBean(ResourceSchemaCache.class)
    ResourceSchemaCache redisResourceSchemaCache(
            StringRedisTemplate redis,
            ObjectMapper objectMapper,
            PlatformSchemaValidationProperties properties
    ) {
        return new RedisResourceSchemaCache(redis, objectMapper, properties.getCache().getRedis().getTtl());
    }
}
