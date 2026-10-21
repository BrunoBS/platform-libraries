package br.com.portalmanager.platform.library.parameterclient.configuration;

import br.com.portalmanager.platform.library.parameterclient.ConfigurationProvider;
import br.com.portalmanager.platform.library.parameterclient.ExpressionProvider;
import br.com.portalmanager.platform.library.parameterclient.internal.RedisConfigurationProvider;
import br.com.portalmanager.platform.library.parameterclient.internal.RedisExpressionProvider;
import br.com.portalmanager.platform.library.parameterclient.internal.RedisParameterReader;
import br.com.portalmanager.platform.library.redis.annotation.RedisNamespace;
import br.com.portalmanager.platform.library.redis.configuration.PlatformRedisAutoConfiguration;
import br.com.portalmanager.platform.library.redis.configuration.RedisNamespaceAutoConfiguration;
import br.com.portalmanager.platform.library.redis.configuration.PlatformRedisProperties.NamespaceMode;
import br.com.portalmanager.platform.library.redis.core.RedisStore;
import br.com.portalmanager.platform.library.redis.serialization.RedisJsonCodec;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;

@AutoConfiguration(after = {PlatformRedisAutoConfiguration.class, RedisNamespaceAutoConfiguration.class})
@ConditionalOnClass(RedisStore.class)
@ConditionalOnProperty(prefix = "platform.parameter-client", name = "enabled", havingValue = "true", matchIfMissing = true)
public class ParameterClientAutoConfiguration {
    @Bean
    @ConditionalOnProperty(prefix = "platform.redis", name = "enabled", havingValue = "true", matchIfMissing = true)
    @ConditionalOnBean({RedisStore.class, RedisJsonCodec.class})
    @ConditionalOnMissingBean(RedisParameterReader.class)
    RedisParameterReader redisParameterReader(
            @RedisNamespace(value = "dynamic-parameter", mode = NamespaceMode.SHARED) RedisStore store,
            RedisJsonCodec codec) {
        return new RedisParameterReader(store, codec);
    }

    @Bean
    @ConditionalOnBean(RedisParameterReader.class)
    @ConditionalOnMissingBean(ConfigurationProvider.class)
    ConfigurationProvider configurationProvider(RedisParameterReader reader) {
        return new RedisConfigurationProvider(reader);
    }

    @Bean
    @ConditionalOnBean(RedisParameterReader.class)
    @ConditionalOnMissingBean(ExpressionProvider.class)
    ExpressionProvider expressionProvider(RedisParameterReader reader) {
        return new RedisExpressionProvider(reader);
    }
}
