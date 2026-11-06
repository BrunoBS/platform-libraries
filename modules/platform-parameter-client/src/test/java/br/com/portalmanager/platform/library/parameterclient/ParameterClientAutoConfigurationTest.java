package br.com.portalmanager.platform.library.parameterclient;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;
import br.com.portalmanager.platform.library.parameterclient.configuration.ParameterClientAutoConfiguration;
import br.com.portalmanager.platform.library.redis.configuration.PlatformRedisAutoConfiguration;
import br.com.portalmanager.platform.library.redis.configuration.PlatformRedisConnectionAutoConfiguration;
import br.com.portalmanager.platform.library.redis.store.RedisStore;
import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.data.redis.autoconfigure.DataRedisAutoConfiguration;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.data.redis.connection.RedisConnection;
import org.springframework.data.redis.connection.RedisConnectionFactory;

class ParameterClientAutoConfigurationTest {
    ApplicationContextRunner context() {
        return contextWithoutRedisConnectionFactory()
                .withBean(RedisConnectionFactory.class, ParameterClientAutoConfigurationTest::healthyConnectionFactory);
    }

    ApplicationContextRunner contextWithoutRedisConnectionFactory() {
        return new ApplicationContextRunner().withConfiguration(AutoConfigurations.of(
                DataRedisAutoConfiguration.class, org.springframework.boot.data.redis.autoconfigure.DataRedisReactiveAutoConfiguration.class,
                PlatformRedisConnectionAutoConfiguration.class, PlatformRedisAutoConfiguration.class, ParameterClientAutoConfiguration.class))
                .withPropertyValues("spring.application.name=consumer");
    }

    static RedisConnectionFactory healthyConnectionFactory() {
        RedisConnectionFactory factory = mock(RedisConnectionFactory.class);
        RedisConnection connection = mock(RedisConnection.class);
        when(factory.getConnection()).thenReturn(connection);
        when(connection.ping()).thenReturn("PONG");
        return factory;
    }

    @Test
    void createsIndependentProvidersWithoutChangingGlobalStore() {
        context().run(ctx -> assertThat(ctx).hasNotFailed()
                .hasSingleBean(ConfigurationProvider.class).hasSingleBean(ExpressionProvider.class)
                .hasSingleBean(RedisStore.class));
    }

    @Test
    void failsToStartWithoutRedisInfrastructure() {
        new ApplicationContextRunner().withConfiguration(AutoConfigurations.of(ParameterClientAutoConfiguration.class))
                .run(ctx -> assertThat(ctx).hasFailed());
        context().withPropertyValues("platform.redis.enabled=false")
                .run(ctx -> assertThat(ctx).hasFailed());
    }

    @Test
    void customRedisStoreWorksWhenPlatformRedisAutoConfigurationIsDisabled() {
        context().withPropertyValues("platform.redis.enabled=false")
                .withBean(RedisStore.class, () -> mock(RedisStore.class))
                .withBean(RedisConnectionFactory.class, ParameterClientAutoConfigurationTest::healthyConnectionFactory)
                .withBean(tools.jackson.databind.ObjectMapper.class,
                        () -> new tools.jackson.databind.json.JsonMapper())
                .run(ctx -> assertThat(ctx).hasNotFailed().hasSingleBean(ConfigurationProvider.class)
                        .hasSingleBean(ExpressionProvider.class));
    }

    @Test
    void failsToStartWhenRedisIsUnavailable() {
        RedisConnectionFactory factory = mock(RedisConnectionFactory.class);
        when(factory.getConnection()).thenThrow(new IllegalStateException("connection refused"));
        contextWithoutRedisConnectionFactory()
                .withBean(RedisConnectionFactory.class, () -> factory)
                .run(ctx -> assertThat(ctx).hasFailed()
                        .getStartupFailure().hasStackTraceContaining("Redis is required by platform-parameter-client"));
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
    void customProvidersStillRequireRedisAtStartup() {
        ConfigurationProvider configuration = mock(ConfigurationProvider.class);
        ExpressionProvider expressions = mock(ExpressionProvider.class);
        new ApplicationContextRunner().withConfiguration(AutoConfigurations.of(ParameterClientAutoConfiguration.class))
                .withBean(ConfigurationProvider.class, () -> configuration)
                .withBean(ExpressionProvider.class, () -> expressions)
                .run(ctx -> assertThat(ctx).hasFailed());
    }
}
