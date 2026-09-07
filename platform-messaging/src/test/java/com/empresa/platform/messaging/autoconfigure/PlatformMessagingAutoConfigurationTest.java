package com.empresa.platform.messaging.autoconfigure;

import com.empresa.platform.messaging.resolver.ApiMessageResolver;
import com.empresa.platform.messaging.web.ApiExceptionHandler;
import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.jdbc.core.JdbcTemplate;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

class PlatformMessagingAutoConfigurationTest {

    private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
            .withConfiguration(AutoConfigurations.of(PlatformMessagingAutoConfiguration.class));

    @Configuration
    static class MockInfrastructureConfiguration {
        @Bean JdbcTemplate jdbcTemplate() { return mock(JdbcTemplate.class); }
    }

    @Test
    void shouldNotLoadBeansWhenDisabled() {
        this.contextRunner
                .withUserConfiguration(MockInfrastructureConfiguration.class)
                .withPropertyValues("platform.messaging.enabled=false")
                .run(context -> {
                    assertThat(context).doesNotHaveBean(ApiMessageResolver.class);
                    assertThat(context).doesNotHaveBean(ApiExceptionHandler.class);
                });
    }

    @Test
    void shouldLoadBeansByParameters() {
        this.contextRunner
                .withUserConfiguration(MockInfrastructureConfiguration.class)
                .withPropertyValues("platform.messaging.enabled=true", "platform.messaging.default-locale=pt-BR")
                .run(context -> {
                    assertThat(context).hasSingleBean(ApiMessageResolver.class);
                    assertThat(context).hasSingleBean(ApiExceptionHandler.class);
                });
    }
}
