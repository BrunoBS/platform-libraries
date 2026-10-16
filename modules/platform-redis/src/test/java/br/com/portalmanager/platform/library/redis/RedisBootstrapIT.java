package br.com.portalmanager.platform.library.redis;

import static org.assertj.core.api.Assertions.*;

import br.com.portalmanager.platform.library.redis.core.*;
import br.com.portalmanager.platform.library.redis.operations.RedisOperations;
import br.com.portalmanager.platform.library.redis.pubsub.RedisPublisher;
import br.com.portalmanager.platform.library.redis.stream.RedisStreams;

import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.context.annotation.Configuration;

class RedisBootstrapIT extends RedisIntegrationSupport {
    @Configuration(proxyBeanMethods = false)
    @EnableAutoConfiguration
    static class DependencyOnly {}

    @Test
    void dependencyAndConnectionPropertiesAreEnoughWithoutManualInfrastructure() {
        new ApplicationContextRunner()
                .withUserConfiguration(DependencyOnly.class)
                .withPropertyValues(
                        "spring.application.name=dependency-only",
                        "spring.data.redis.host=" + REDIS.getHost(),
                        "spring.data.redis.port=" + REDIS.getMappedPort(6379))
                .run(
                        ctx -> {
                            assertThat(ctx)
                                    .hasSingleBean(RedisStore.class)
                                    .hasSingleBean(AsyncRedisStore.class)
                                    .hasSingleBean(RedisOperations.class)
                                    .hasSingleBean(RedisPublisher.class)
                                    .hasSingleBean(RedisStreams.class);
                            String key = key();
                            ctx.getBean(RedisStore.class).set(key, new Event("boot", 1));
                            assertThat(ctx.getBean(RedisStore.class).get(key, Event.class))
                                    .contains(new Event("boot", 1));
                        });
    }

    @Test
    void disabledModuleAndInvalidConfigurationAreExplicit() {
        context()
                .withPropertyValues("platform.redis.enabled=false")
                .run(ctx -> assertThat(ctx).doesNotHaveBean(RedisStore.class));
        context()
                .withPropertyValues("platform.redis.max-read-size=0")
                .run(ctx -> assertThat(ctx).hasFailed());
    }
}
