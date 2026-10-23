package br.com.portalmanager.platform.library.testing.redis;

import static org.assertj.core.api.Assertions.*;

import br.com.portalmanager.platform.library.testing.redis.annotation.WithRedis;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringBootConfiguration;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.core.env.Environment;
import java.util.concurrent.atomic.AtomicReference;

import org.springframework.data.redis.core.StringRedisTemplate;

@SpringBootTest(
        classes = RedisFixtureIT.Application.class,
        properties = {
            "spring.application.name=redis-fixture",
            "spring.data.redis.cluster.nodes=127.0.0.1:1",
            "spring.data.redis.sentinel.master=unused",
            "spring.data.redis.sentinel.nodes=127.0.0.1:2"
        })
@WithRedis
@org.junit.jupiter.api.TestMethodOrder(org.junit.jupiter.api.MethodOrderer.OrderAnnotation.class)
class RedisFixtureIT {
    private static final AtomicReference<org.testcontainers.containers.GenericContainer<?>>
            SHARED_CONTAINER = new AtomicReference<>();

    static void assertSharedContainer(
            org.testcontainers.containers.GenericContainer<?> candidate) {
        SHARED_CONTAINER.compareAndSet(null, candidate);
        assertThat(candidate).isSameAs(SHARED_CONTAINER.get());
    }

    @SpringBootConfiguration
    @EnableAutoConfiguration
    static class Application {}

    @org.junit.jupiter.api.BeforeAll
    static void connectionMatchesContainer(
            @Autowired
                    org.springframework.data.redis.connection.lettuce.LettuceConnectionFactory
                            factory,
            @Autowired org.testcontainers.containers.GenericContainer<?> container)
            throws Exception {
        assertSharedContainer(container);
        assertThat(factory.getClusterConfiguration()).isNull();
        assertThat(factory.getSentinelConfiguration()).isNull();
        assertThat(factory.getHostName()).isEqualTo(container.getHost());
        assertThat(factory.getPort()).isEqualTo(container.getMappedPort(6379));
        assertThat(container.execInContainer("redis-cli", "PING").getStdout()).contains("PONG");
    }

    @Autowired StringRedisTemplate redis;
    @Autowired Environment environment;

    @Test
    @org.junit.jupiter.api.Order(1)
    void startsAndConfiguresConnectionAndExclusiveNamespace() {
        assertThat(environment.getProperty("platform.redis.namespace")).startsWith("test-");
        redis.opsForValue().set("other-service:untouched", "preserved");
        redis.opsForValue()
                .set(
                        environment.getProperty("platform.redis.namespace")
                                + ".redis-fixture:fixture",
                        "value");
        assertThat(
                        redis.opsForValue()
                                .get(
                                        environment.getProperty("platform.redis.namespace")
                                                + ".redis-fixture:fixture"))
                .isEqualTo("value");
    }

    @Test
    @org.junit.jupiter.api.Order(2)
    void cleansOnlyThisNamespaceBeforeNextTest() {
        assertThat(redis.opsForValue().get("other-service:untouched")).isEqualTo("preserved");
        assertThat(
                        redis.opsForValue()
                                .get(
                                        environment.getProperty("platform.redis.namespace")
                                                + ".redis-fixture:fixture"))
                .isNull();
    }
}
