package br.com.portalmanager.platform.library.audit.autoconfigure;

import br.com.portalmanager.platform.library.audit.aspect.AuditAspect;
import br.com.portalmanager.platform.library.audit.config.PlatformAuditProperties;
import br.com.portalmanager.platform.library.audit.outbox.AuditOutboxStore;
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
        @Bean ObjectMapper objectMapper() { return new ObjectMapper(); }
        @Bean AuditOutboxStore auditOutboxStore() { return mock(AuditOutboxStore.class); }
    }

    @Test
    void shouldLoadTransactionalCaptureWhenEnabledAndConfigured() {
        contextRunner
                .withPropertyValues("platform.audit.enabled=true", "platform.audit.service-name=account")
                .run(context -> {
                    assertThat(context).hasSingleBean(AuditAspect.class);
                    assertThat(context).hasSingleBean(PlatformAuditProperties.class);
                });
    }

    @Test
    void shouldFailStartupWhenServiceNameIsMissing() {
        contextRunner
                .withPropertyValues("platform.audit.enabled=true")
                .run(context -> assertThat(context).hasFailed());
    }

    @Test
    void shouldRejectNonPositiveBoundsAtStartup() {
        contextRunner
                .withPropertyValues(
                        "platform.audit.enabled=true",
                        "platform.audit.service-name=account",
                        "platform.audit.max-event-size-bytes=0"
                )
                .run(context -> assertThat(context).hasFailed());
    }

    @Test
    void shouldNotLoadAuditInfrastructureWhenDisabled() {
        contextRunner
                .withPropertyValues("platform.audit.enabled=false")
                .run(context -> {
                    assertThat(context).doesNotHaveBean(AuditAspect.class);
                    assertThat(context).doesNotHaveBean(PlatformAuditProperties.class);
                });
    }
}
