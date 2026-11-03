package br.com.portalmanager.platform.library.redis;

import static org.assertj.core.api.Assertions.*;

import br.com.portalmanager.platform.library.redis.store.*;
import br.com.portalmanager.platform.library.redis.operations.*;
import br.com.portalmanager.platform.library.redis.pubsub.*;
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
    void disabledModuleAndInvalidConfigurationAreExplicit() {
        context()
                .withPropertyValues("platform.redis.enabled=false")
                .run(ctx -> assertThat(ctx).doesNotHaveBean(RedisStore.class));
        context()
                .withPropertyValues("platform.redis.limits.max-read-size=0")
                .run(ctx -> assertThat(ctx).hasFailed());
    }
}
