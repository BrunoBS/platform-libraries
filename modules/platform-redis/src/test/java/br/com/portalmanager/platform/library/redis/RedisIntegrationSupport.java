package br.com.portalmanager.platform.library.redis;

import br.com.portalmanager.platform.library.redis.configuration.PlatformRedisAutoConfiguration;
import br.com.portalmanager.platform.library.redis.configuration.PlatformRedisConnectionAutoConfiguration;

import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.data.redis.autoconfigure.*;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.testcontainers.containers.GenericContainer;

@br.com.portalmanager.platform.library.testing.redis.annotation.WithRedis
@org.junit.jupiter.api.extension.ExtendWith(
        org.springframework.test.context.junit.jupiter.SpringExtension.class)
@org.springframework.test.context.ContextConfiguration(
        classes = br.com.portalmanager.platform.library.testing.redis.RedisTestConfiguration.class)
abstract class RedisIntegrationSupport {
    static GenericContainer<?> REDIS;

    @org.junit.jupiter.api.BeforeAll
    static void fixture(
            @org.springframework.beans.factory.annotation.Autowired GenericContainer<?> container) {
        REDIS = container;
    }

    ApplicationContextRunner context() {
        return new ApplicationContextRunner()
                .withConfiguration(
                        AutoConfigurations.of(
                                DataRedisAutoConfiguration.class,
                                DataRedisReactiveAutoConfiguration.class,
                                PlatformRedisConnectionAutoConfiguration.class, PlatformRedisAutoConfiguration.class))
                .withPropertyValues(
                        "spring.application.name=redis-test",
                        "platform.redis.connection.host=" + REDIS.getHost(),
                        "platform.redis.connection.port=" + REDIS.getMappedPort(6379),
                        "platform.redis.connection.command-timeout=1s",
                        "platform.redis.connection.connect-timeout=1s");
    }

    String key() {
        return java.util.UUID.randomUUID().toString();
    }

    record Event(String name, int count) {}
}
