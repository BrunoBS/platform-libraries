package br.com.portalmanager.platform.library.testing.redis;

import br.com.portalmanager.platform.library.messaging.exception.PlatformConfigurationException;
import br.com.portalmanager.platform.library.testing.container.PinnedDockerImage;

import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.env.MapPropertySource;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.utility.DockerImageName;

import java.util.Map;

@TestConfiguration(proxyBeanMethods = false)
public class RedisTestConfiguration {
    @Bean(destroyMethod = "stop")
    GenericContainer<?> redisTestContainer(ConfigurableEnvironment environment) {
        GenericContainer<?> container;
        try {
            String image =
                    PinnedDockerImage.validatePinnedImage(
                            environment.getProperty(
                                    "platform.testing.redis.image", RedisContainerImages.REDIS));
            container = new GenericContainer<>(DockerImageName.parse(image)).withExposedPorts(6379);
        } catch (IllegalArgumentException failure) {
            throw new PlatformConfigurationException(
                    RedisTestingTechnicalErrors.invalidConfiguration());
        }
        try {
            container.start();
        } catch (RuntimeException failure) {
            try {
                container.stop();
            } catch (RuntimeException ignored) {
            }
            throw new PlatformConfigurationException(
                    RedisTestingTechnicalErrors.startupFailed(),
                    new Exception(failure.getClass().getSimpleName()));
        }
        environment
                .getPropertySources()
                .addFirst(
                        new MapPropertySource(
                                "platformTestingRedisConnection",
                                Map.of(
                                        "platform.redis.connection.host",
                                        container.getHost(),
                                        "platform.redis.connection.port",
                                        container.getMappedPort(6379),
                                        "platform.redis.connection.database",
                                        0,
                                        "platform.redis.connection.ssl.enabled",
                                        false,
                                        // This module also supports tests that use Spring Data Redis
                                        // without Platform Redis being on the classpath.
                                        "spring.data.redis.host",
                                        container.getHost(),
                                        "spring.data.redis.port",
                                        container.getMappedPort(6379),
                                        "spring.data.redis.database",
                                        0,
                                        "spring.data.redis.ssl.enabled",
                                        false)));
        return container;
    }

    @Bean
    static org.springframework.beans.factory.config.BeanFactoryPostProcessor
            redisTestConnectionInitializer() {
        // Establish dynamic Platform Redis properties before connection details/factories are created.
        return factory -> factory.getBean("redisTestContainer");
    }
}
