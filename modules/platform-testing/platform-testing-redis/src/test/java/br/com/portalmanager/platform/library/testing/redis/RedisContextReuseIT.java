package br.com.portalmanager.platform.library.testing.redis;

import br.com.portalmanager.platform.library.testing.redis.annotation.WithRedis;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.testcontainers.containers.GenericContainer;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(
        classes = RedisFixtureIT.Application.class,
        properties = {
            "spring.application.name=redis-fixture",
            "spring.data.redis.cluster.nodes=127.0.0.1:1",
            "spring.data.redis.sentinel.master=unused",
            "spring.data.redis.sentinel.nodes=127.0.0.1:2"
        })
@WithRedis
class RedisContextReuseIT {
    @BeforeAll
    static void reusesTheContainerStartedByTheEquivalentFixture(
            @Autowired GenericContainer<?> container) {
        RedisFixtureIT.assertSharedContainer(container);
    }

    @Test
    void contextReuseKeepsRedisAvailable(@Autowired GenericContainer<?> container) {
        assertThat(container.isRunning()).isTrue();
    }
}
