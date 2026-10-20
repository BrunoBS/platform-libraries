package br.com.portalmanager.platform.library.parameterclient;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;
import br.com.portalmanager.platform.library.parameterclient.configuration.ParameterClientAutoConfiguration;
import br.com.portalmanager.platform.library.redis.configuration.PlatformRedisProperties;
import br.com.portalmanager.platform.library.redis.core.RedisStore;
import br.com.portalmanager.platform.library.redis.serialization.RedisJsonCodec;
import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.context.annotation.Configuration;
import tools.jackson.databind.json.JsonMapper;

class ParameterClientAutoConfigurationTest {
    ApplicationContextRunner context() {
        return new ApplicationContextRunner()
                .withConfiguration(AutoConfigurations.of(ParameterClientAutoConfiguration.class))
                .withUserConfiguration(Properties.class)
                .withPropertyValues("platform.redis.namespace-mode=SHARED", "platform.redis.namespace=dynamic-parameter")
                .withBean(RedisJsonCodec.class, () -> new RedisJsonCodec(new JsonMapper(), 1048576));
    }

    @Test
    void createsOneProviderWithSharedBeans() {
        RedisStore store = mock(RedisStore.class);
        context().withBean(RedisStore.class, () -> store).run(ctx -> {
            assertThat(ctx).hasSingleBean(ParameterProvider.class).hasSingleBean(RedisStore.class);
            assertThat(ctx.getBean(ParameterProvider.class).get("platform.absent", 30)).isEqualTo(30);
            verify(store).get("platform.absent", tools.jackson.databind.JsonNode.class);
        });
    }

    @Test
    void backsOffWithoutRedisOrWhenDisabled() {
        context().run(ctx -> assertThat(ctx).doesNotHaveBean(ParameterProvider.class));
        context().withBean(RedisStore.class, () -> mock(RedisStore.class))
                .withPropertyValues("platform.parameter-client.enabled=false")
                .run(ctx -> assertThat(ctx).doesNotHaveBean(ParameterProvider.class));
    }

    @Test
    void preservesCustomProviderWithoutValidatingUnusedNamespace() {
        ParameterProvider custom = mock(ParameterProvider.class);
        context().withBean(RedisStore.class, () -> mock(RedisStore.class))
                .withPropertyValues("platform.redis.namespace-mode=ISOLATED")
                .withBean(ParameterProvider.class, () -> custom)
                .run(ctx -> assertThat(ctx.getBean(ParameterProvider.class)).isSameAs(custom));
    }

    @Test
    void refusesSilentReadsInWrongNamespace() {
        for (String property : new String[]{"platform.redis.namespace=config", "platform.redis.namespace-mode=ISOLATED"}) {
            context().withBean(RedisStore.class, () -> mock(RedisStore.class)).withPropertyValues(property)
                    .run(ctx -> assertThat(ctx).hasFailed());
        }
    }

    @Configuration(proxyBeanMethods = false)
    @EnableConfigurationProperties(PlatformRedisProperties.class)
    static class Properties {}
}
