package br.com.portalmanager.platform.library.parameterclient.configuration;

import br.com.portalmanager.platform.library.messaging.exception.PlatformConfigurationException;
import br.com.portalmanager.platform.library.messaging.model.PlatformErrorDefinition;
import br.com.portalmanager.platform.library.parameterclient.ParameterProvider;
import br.com.portalmanager.platform.library.parameterclient.internal.RedisParameterProvider;
import br.com.portalmanager.platform.library.redis.configuration.PlatformRedisAutoConfiguration;
import br.com.portalmanager.platform.library.redis.configuration.PlatformRedisProperties;
import br.com.portalmanager.platform.library.redis.core.RedisStore;
import br.com.portalmanager.platform.library.redis.serialization.RedisJsonCodec;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;

@AutoConfiguration(after = PlatformRedisAutoConfiguration.class)
@ConditionalOnClass(RedisStore.class)
@ConditionalOnProperty(prefix = "platform.parameter-client", name = "enabled", havingValue = "true", matchIfMissing = true)
public class ParameterClientAutoConfiguration {
    @Bean
    @ConditionalOnBean({RedisStore.class, RedisJsonCodec.class, PlatformRedisProperties.class})
    @ConditionalOnMissingBean(ParameterProvider.class)
    ParameterProvider parameterProvider(RedisStore store, RedisJsonCodec codec, PlatformRedisProperties properties) {
        if (properties.namespaceMode() != PlatformRedisProperties.NamespaceMode.SHARED
                || !"dynamic-parameter".equals(properties.namespace())) {
            throw new PlatformConfigurationException(new PlatformErrorDefinition(
                    "PLT-PARAM-005", "Invalid parameter client Redis namespace.",
                    "Use platform.redis.namespace-mode=SHARED and namespace=dynamic-parameter.", 500));
        }
        return new RedisParameterProvider(store, codec);
    }
}
