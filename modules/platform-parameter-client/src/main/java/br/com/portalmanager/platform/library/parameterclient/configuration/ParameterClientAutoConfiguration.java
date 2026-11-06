package br.com.portalmanager.platform.library.parameterclient.configuration;

import br.com.portalmanager.platform.library.parameterclient.ConfigurationProvider;
import br.com.portalmanager.platform.library.parameterclient.ExpressionProvider;
import br.com.portalmanager.platform.library.parameterclient.internal.RedisConfigurationProvider;
import br.com.portalmanager.platform.library.parameterclient.internal.RedisExpressionProvider;
import br.com.portalmanager.platform.library.parameterclient.internal.RedisParameterReader;
import br.com.portalmanager.platform.library.parameterclient.message.ParameterClientTechnicalErrors;
import br.com.portalmanager.platform.library.messaging.exception.PlatformConfigurationException;
import br.com.portalmanager.platform.library.redis.configuration.PlatformRedisAutoConfiguration;
import br.com.portalmanager.platform.library.redis.store.RedisStore;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.data.redis.connection.RedisConnection;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.json.JsonMapper;

@AutoConfiguration(after = PlatformRedisAutoConfiguration.class)
@ConditionalOnClass(RedisStore.class)
public class ParameterClientAutoConfiguration {
    @Bean
    @ConditionalOnMissingBean(RedisParameterReader.class)
    RedisParameterReader redisParameterReader(
            ObjectProvider<RedisStore> storeProvider,
            ObjectProvider<RedisConnectionFactory> connectionFactoryProvider,
            ObjectProvider<ObjectMapper> mapper) {
        RedisStore store = storeProvider.getIfAvailable();
        RedisConnectionFactory connectionFactory = connectionFactoryProvider.getIfAvailable();
        if (store == null || connectionFactory == null) {
            throw new PlatformConfigurationException(ParameterClientTechnicalErrors.REDIS_REQUIRED);
        }
        try (RedisConnection connection = connectionFactory.getConnection()) {
            connection.ping();
        } catch (RuntimeException failure) {
            throw new PlatformConfigurationException(
                    ParameterClientTechnicalErrors.REDIS_REQUIRED, failure);
        }
        return new RedisParameterReader(store, mapper.getIfAvailable(JsonMapper::new));
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
