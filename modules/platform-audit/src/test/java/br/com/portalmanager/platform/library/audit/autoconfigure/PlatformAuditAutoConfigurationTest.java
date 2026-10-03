package br.com.portalmanager.platform.library.audit.autoconfigure;

import br.com.portalmanager.platform.library.audit.aspect.AuditAspect;
import br.com.portalmanager.platform.library.audit.context.AuditAuthorizationContextResolver;
import br.com.portalmanager.platform.library.audit.queue.AuditEventQueue;
import br.com.portalmanager.platform.library.audit.publisher.AuditPublisher;
import br.com.portalmanager.platform.library.messaging.exception.PlatformConfigurationException;
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
        @Bean RestClient.Builder restClientBuilder() { return RestClient.builder(); }
        @Bean HttpServletRequest httpServletRequest() { return mock(HttpServletRequest.class); }
        @Bean ObjectMapper objectMapper() { return new ObjectMapper(); }
    }

    @Configuration
    static class QueueInfrastructure {
        @Bean AuditEventQueue auditEventQueue() { return mock(AuditEventQueue.class); }
    }

    @Test
    void shouldLoadAuditInfrastructureWithQueue() {
        contextRunner
                .withUserConfiguration(QueueInfrastructure.class)
                .withPropertyValues(
                        "platform.audit.enabled=true",
                        "platform.audit.service-url=http://audit-api",
                        "platform.audit.service-name=account"
                )
                .run(context -> {
                    assertThat(context).hasSingleBean(AuditPublisher.class);
                    assertThat(context).hasSingleBean(AuditAuthorizationContextResolver.class);
                    assertThat(context).hasSingleBean(AuditAspect.class);
                });
    }

    @Test
    void shouldFailStartupWhenServiceUrlIsMissing() {
        contextRunner
                .withUserConfiguration(QueueInfrastructure.class)
                .withPropertyValues(
                        "platform.audit.enabled=true",
                        "platform.audit.service-name=account"
                )
                .run(context -> {
                    assertThat(context.getStartupFailure()).isNotNull();
                    assertThat(context.getStartupFailure())
                            .hasRootCauseInstanceOf(PlatformConfigurationException.class)
                            .hasRootCauseMessage(
                                    "platform.audit.service-url is required when platform.audit is enabled"
                            );
                });
    }

    @Test
    void shouldFailStartupWhenQueueIsMissingInAsynchronousMode() {
        contextRunner
                .withPropertyValues(
                        "platform.audit.enabled=true",
                        "platform.audit.service-url=http://audit-api",
                        "platform.audit.fail-on-error=false"
                )
                .run(context -> {
                    assertThat(context.getStartupFailure()).isNotNull();
                    assertThat(context.getStartupFailure())
                            .isInstanceOf(PlatformConfigurationException.class)
                            .hasMessage(
                                    "Audit event queue is required for asynchronous at-least-once delivery"
                            );
                });
    }

    @Test
    void shouldAllowStrictModeWithoutQueue() {
        contextRunner
                .withPropertyValues(
                        "platform.audit.enabled=true",
                        "platform.audit.service-url=http://audit-api",
                        "platform.audit.fail-on-error=true",
                        "platform.audit.queue.enabled=false"
                )
                .run(context -> assertThat(context).hasSingleBean(AuditPublisher.class));
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
