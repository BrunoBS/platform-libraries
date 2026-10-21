package br.com.portalmanager.platform.library.parameterclient;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;
import br.com.portalmanager.platform.library.parameterclient.configuration.ParameterClientAutoConfiguration;
import br.com.portalmanager.platform.library.redis.configuration.PlatformRedisAutoConfiguration;
import br.com.portalmanager.platform.library.redis.configuration.RedisNamespaceAutoConfiguration;
import br.com.portalmanager.platform.library.redis.core.RedisStore;
import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.data.redis.autoconfigure.DataRedisAutoConfiguration;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;

class ParameterClientAutoConfigurationTest {
    ApplicationContextRunner context() {
        return new ApplicationContextRunner().withConfiguration(AutoConfigurations.of(
                DataRedisAutoConfiguration.class, org.springframework.boot.data.redis.autoconfigure.DataRedisReactiveAutoConfiguration.class, RedisNamespaceAutoConfiguration.class,
                PlatformRedisAutoConfiguration.class, ParameterClientAutoConfiguration.class))
                .withPropertyValues("spring.application.name=consumer",
                        "platform.redis.namespace-mode=ISOLATED", "platform.redis.namespace=orders");
    }

    @Test
    void createsIndependentProvidersWithoutChangingGlobalStore() {
        context().run(ctx -> assertThat(ctx).hasNotFailed()
                .hasSingleBean(ConfigurationProvider.class).hasSingleBean(ExpressionProvider.class)
                .hasSingleBean(RedisStore.class));
    }

    @Test
    void backsOffWithoutRedisInfrastructureOrWhenClientIsDisabled() {
        new ApplicationContextRunner().withConfiguration(AutoConfigurations.of(ParameterClientAutoConfiguration.class))
                .run(ctx -> assertThat(ctx).doesNotHaveBean(ConfigurationProvider.class)
                        .doesNotHaveBean(ExpressionProvider.class));
        context().withPropertyValues("platform.parameter-client.enabled=false")
                .run(ctx -> assertThat(ctx).hasSingleBean(RedisStore.class)
                        .doesNotHaveBean(ConfigurationProvider.class).doesNotHaveBean(ExpressionProvider.class));
        context().withPropertyValues("platform.redis.enabled=false")
                .run(ctx -> assertThat(ctx).doesNotHaveBean(RedisStore.class)
                        .doesNotHaveBean(ConfigurationProvider.class).doesNotHaveBean(ExpressionProvider.class));
    }

    @Test
    void disabledRedisDoesNotCreateProvidersEvenWithCustomStoreAndCodec() {
        context().withPropertyValues("platform.redis.enabled=false")
                .withBean(RedisStore.class, () -> mock(RedisStore.class))
                .withBean(br.com.portalmanager.platform.library.redis.serialization.RedisJsonCodec.class,
                        () -> new br.com.portalmanager.platform.library.redis.serialization.RedisJsonCodec(
                                new tools.jackson.databind.json.JsonMapper(), 1048576))
                .run(ctx -> assertThat(ctx).hasNotFailed().doesNotHaveBean(ConfigurationProvider.class)
                        .doesNotHaveBean(ExpressionProvider.class));
    }

    @Test
    void customConfigurationDoesNotSuppressDefaultExpressionProvider() {
        ConfigurationProvider custom = mock(ConfigurationProvider.class);
        context().withBean(ConfigurationProvider.class, () -> custom).run(ctx -> {
            assertThat(ctx.getBean(ConfigurationProvider.class)).isSameAs(custom);
            assertThat(ctx).hasSingleBean(ExpressionProvider.class);
        });
    }

    @Test
    void customExpressionDoesNotSuppressDefaultConfigurationProvider() {
        ExpressionProvider custom = mock(ExpressionProvider.class);
        context().withBean(ExpressionProvider.class, () -> custom).run(ctx -> {
            assertThat(ctx.getBean(ExpressionProvider.class)).isSameAs(custom);
            assertThat(ctx).hasSingleBean(ConfigurationProvider.class);
        });
    }

    @Test
    void customProvidersRemainIndependentlyInjectableWithoutRedis() {
        ConfigurationProvider configuration = mock(ConfigurationProvider.class);
        ExpressionProvider expressions = mock(ExpressionProvider.class);
        new ApplicationContextRunner().withConfiguration(AutoConfigurations.of(ParameterClientAutoConfiguration.class))
                .withBean(ConfigurationProvider.class, () -> configuration)
                .withBean(ExpressionProvider.class, () -> expressions).run(ctx -> {
                    assertThat(ctx.getBean(ConfigurationProvider.class)).isSameAs(configuration);
                    assertThat(ctx.getBean(ExpressionProvider.class)).isSameAs(expressions);
                });
    }
}
