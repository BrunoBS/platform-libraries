package br.com.portalmanager.platform.audit.autoconfigure;

import br.com.portalmanager.platform.audit.aspect.AuditAspect;
import br.com.portalmanager.platform.audit.context.AuditAuthorizationContextResolver;
import br.com.portalmanager.platform.audit.message.AuditMessageKeys;
import br.com.portalmanager.platform.audit.publisher.AuditPublisher;
import jakarta.servlet.http.HttpServletRequest;
import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestClient;
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
        RestClient.Builder restClientBuilder() {
            return RestClient.builder();
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
                        "platform.audit.service-url=http://audit-api",
                        "platform.audit.service-name=account"
                )
                .run(context -> {
                    assertThat(context).hasSingleBean(AuditPublisher.class);
                    assertThat(context).hasSingleBean(AuditAuthorizationContextResolver.class);
                    assertThat(context).hasSingleBean(AuditAspect.class);
                    assertThat(context).hasBean("platformAuditTaskExecutor");
                });
    }


    @Test
    void shouldFailStartupWhenServiceUrlIsMissing() {
        contextRunner
                .withPropertyValues(
                        "platform.audit.enabled=true",
                        "platform.audit.service-name=account"
                )
                .run(context -> {
                    assertThat(context.getStartupFailure()).isNotNull();
                    assertThat(context.getStartupFailure())
                            .hasStackTraceContaining(AuditMessageKeys.SERVICE_URL_REQUIRED);
                });
    }

    @Test
    void shouldFailStartupWhenFallbackIsEnabledWithoutStore() {
        contextRunner
                .withPropertyValues(
                        "platform.audit.enabled=true",
                        "platform.audit.service-url=http://audit-api",
                        "platform.audit.fallback.enabled=true"
                )
                .run(context -> {
                    assertThat(context.getStartupFailure()).isNotNull();
                    assertThat(context.getStartupFailure())
                            .hasStackTraceContaining(AuditMessageKeys.FALLBACK_STORE_MISSING);
                });
    }

    @Test
    void shouldNotLoadAuditInfrastructureWhenDisabled() {
        contextRunner
                .withPropertyValues("platform.audit.enabled=false")
                .run(context -> {
                    assertThat(context).doesNotHaveBean(AuditPublisher.class);
                    assertThat(context).doesNotHaveBean(AuditAuthorizationContextResolver.class);
                    assertThat(context).doesNotHaveBean(AuditAspect.class);
                });
    }
}
