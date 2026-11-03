package br.com.portalmanager.platform.library.redis.configuration;

import br.com.portalmanager.platform.library.redis.configuration.PlatformRedisProperties.Connection;
import br.com.portalmanager.platform.library.redis.configuration.PlatformRedisProperties.Topology;
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

import java.util.List;

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
                if (connection.topology() != Topology.STANDALONE) return null;
                return Standalone.of(connection.host(), connection.port(), connection.database());
            }

            @Override
            public Cluster getCluster() {
                if (connection.topology() != Topology.CLUSTER) return null;
                List<Node> nodes = connection.cluster().nodes().stream()
                        .map(PlatformRedisConnectionAutoConfiguration::node)
                        .toList();
                return () -> nodes;
            }

            @Override
            public Sentinel getSentinel() {
                if (connection.topology() != Topology.SENTINEL) return null;
                List<Node> nodes = connection.sentinel().nodes().stream()
                        .map(PlatformRedisConnectionAutoConfiguration::node)
                        .toList();
                return new Sentinel() {
                    @Override public String getMaster() { return connection.sentinel().master(); }
                    @Override public List<Node> getNodes() { return nodes; }
                    @Override public int getDatabase() { return connection.sentinel().database(); }
                    @Override public String getUsername() { return nonBlank(connection.sentinel().username()); }
                    @Override public String getPassword() { return nonBlank(connection.sentinel().password()); }
                };
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

    private static DataRedisConnectionDetails.Node node(String address) {
        if (address == null || address.isBlank()) throw RedisErrors.configuration();
        String host;
        String portText;
        if (address.startsWith("[")) {
            int closingBracket = address.indexOf(']');
            if (closingBracket < 0
                    || closingBracket + 1 >= address.length()
                    || address.charAt(closingBracket + 1) != ':') {
                throw RedisErrors.configuration();
            }
            host = address.substring(1, closingBracket);
            portText = address.substring(closingBracket + 2);
        } else {
            int separator = address.lastIndexOf(':');
            if (separator < 1) throw RedisErrors.configuration();
            host = address.substring(0, separator);
            portText = address.substring(separator + 1);
        }
        try {
            int port = Integer.parseInt(portText);
            if (host.isBlank() || port < 1 || port > 65535) throw RedisErrors.configuration();
            return new DataRedisConnectionDetails.Node(host, port);
        } catch (NumberFormatException invalid) {
            throw RedisErrors.configuration();
        }
    }

    private static String nonBlank(String value) {
        return value == null || value.isBlank() ? null : value;
    }
}
