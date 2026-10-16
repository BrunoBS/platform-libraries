package br.com.portalmanager.platform.library.redis;

import br.com.portalmanager.platform.library.redis.configuration.PlatformRedisAutoConfiguration;

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
                                PlatformRedisAutoConfiguration.class))
                .withPropertyValues(
                        "spring.application.name=redis-test",
                        "spring.data.redis.host=" + REDIS.getHost(),
                        "spring.data.redis.port=" + REDIS.getMappedPort(6379),
                        "spring.data.redis.timeout=1s",
                        "spring.data.redis.connect-timeout=1s");
    }

    String key() {
        return java.util.UUID.randomUUID().toString();
    }

    record Event(String name, int count) {}
}
