package br.com.portalmanager.platform.library.redis.configuration;

import br.com.portalmanager.platform.library.redis.api.configuration.PlatformRedisProperties;
import br.com.portalmanager.platform.library.redis.api.configuration.PlatformRedisProperties.Connection;
import br.com.portalmanager.platform.library.redis.internal.common.RedisErrors;

import io.lettuce.core.SocketOptions;

import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.data.redis.autoconfigure.DataRedisConnectionDetails;
import org.springframework.boot.data.redis.autoconfigure.LettuceClientConfigurationBuilderCustomizer;
import org.springframework.boot.data.redis.autoconfigure.LettuceClientOptionsBuilderCustomizer;
import org.springframework.boot.ssl.SslBundle;
import org.springframework.boot.ssl.SslBundles;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.context.annotation.Bean;

@AutoConfiguration(
        beforeName = {
            "org.springframework.boot.data.redis.autoconfigure.DataRedisAutoConfiguration"
        })
@ConditionalOnClass(DataRedisConnectionDetails.class)
@ConditionalOnProperty(
        prefix = "platform.redis", name = "enabled", havingValue = "true", matchIfMissing = true)
public class PlatformRedisConnectionAutoConfiguration {
    @Bean
    @ConditionalOnMissingBean(DataRedisConnectionDetails.class)
    DataRedisConnectionDetails platformRedisConnectionDetails(
            PlatformRedisProperties properties, ObjectProvider<SslBundles> sslBundlesProvider) {
        Connection connection = properties.connection();
        return new DataRedisConnectionDetails() {
            @Override
            public String getUsername() {
                return nonBlank(connection.username());
            }

            @Override
            public String getPassword() {
                return nonBlank(connection.password());
            }

            @Override
            public SslBundle getSslBundle() {
                if (!connection.ssl().enabled()) return null;
                String bundleName = connection.ssl().bundle();
                if (bundleName == null || bundleName.isBlank()) return null;
                SslBundles sslBundles = sslBundlesProvider.getIfAvailable();
                if (sslBundles == null) throw RedisErrors.configuration();
                return sslBundles.getBundle(bundleName);
            }

            @Override
            public Standalone getStandalone() {
                return Standalone.of(connection.host(), connection.port(), connection.database());
            }
        };
    }

    @Bean
    @ConditionalOnClass(LettuceClientConfigurationBuilderCustomizer.class)
    LettuceClientConfigurationBuilderCustomizer platformRedisLettuceClientConfiguration(
            PlatformRedisProperties properties) {
        Connection connection = properties.connection();
        return builder -> {
            builder.commandTimeout(connection.commandTimeout());
            if (connection.ssl().enabled()) builder.useSsl();
        };
    }

    @Bean
    @ConditionalOnClass(LettuceClientOptionsBuilderCustomizer.class)
    LettuceClientOptionsBuilderCustomizer platformRedisLettuceClientOptions(
            PlatformRedisProperties properties) {
        return builder ->
                builder.socketOptions(
                        SocketOptions.builder()
                                .connectTimeout(properties.connection().connectTimeout())
                                .build());
    }

    private static String nonBlank(String value) {
        return value == null || value.isBlank() ? null : value;
    }
}
