package com.empresa.platform.audit.autoconfigure;

import com.empresa.platform.audit.aspect.AuditAspect;
import com.empresa.platform.audit.client.AuditEventClient;
import com.empresa.platform.audit.context.AuditContextProvider;
import com.empresa.platform.audit.publisher.AuditPublisher;
import jakarta.servlet.http.HttpServletRequest;
import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import tools.jackson.databind.ObjectMapper;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

class PlatformAuditAutoConfigurationTest {

    private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
            .withConfiguration(AutoConfigurations.of(PlatformAuditAutoConfiguration.class))
            .withUserConfiguration(TestInfrastructure.class);

    @Configuration
    static class TestInfrastructure {

        @Bean
        AuditEventClient auditEventClient() {
            return mock(AuditEventClient.class);
        }

        @Bean
        HttpServletRequest httpServletRequest() {
            return mock(HttpServletRequest.class);
        }

        @Bean
        ObjectMapper objectMapper() {
            return new ObjectMapper();
        }
    }

    @Test
    void shouldLoadAuditInfrastructureWhenEnabled() {
        contextRunner
                .withPropertyValues(
                        "platform.audit.enabled=true",
                        "platform.audit.service-name=account"
                )
                .run(context -> {
                    assertThat(context).hasSingleBean(AuditPublisher.class);
                    assertThat(context).hasSingleBean(AuditContextProvider.class);
                    assertThat(context).hasSingleBean(AuditAspect.class);
                    assertThat(context).hasBean("platformAuditTaskExecutor");
                });
    }

    @Test
    void shouldFailStartupWhenFallbackIsEnabledWithoutStore() {
        contextRunner
                .withPropertyValues(
                        "platform.audit.enabled=true",
                        "platform.audit.fallback.enabled=true"
                )
                .run(context ->
                        assertThat(context.getStartupFailure())
                                .isInstanceOf(IllegalStateException.class)
                                .hasMessageContaining("AuditFallbackStore")
                );
    }

    @Test
    void shouldNotLoadAuditInfrastructureWhenDisabled() {
        contextRunner
                .withPropertyValues("platform.audit.enabled=false")
                .run(context -> {
                    assertThat(context).doesNotHaveBean(AuditPublisher.class);
                    assertThat(context).doesNotHaveBean(AuditContextProvider.class);
                    assertThat(context).doesNotHaveBean(AuditAspect.class);
                });
    }
}
