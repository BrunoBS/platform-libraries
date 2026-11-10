package br.com.portalmanager.platform.library.audit.configuration;

import br.com.portalmanager.platform.library.audit.internal.aspect.AuditAspect;
import br.com.portalmanager.platform.library.audit.api.configuration.PlatformAuditProperties;
import br.com.portalmanager.platform.library.audit.internal.outbox.AuditOutboxAppender;
import br.com.portalmanager.platform.library.audit.api.contract.outbox.AuditOutboxRepository;
import br.com.portalmanager.platform.library.objectstorage.api.configuration.ObjectStorageProperties;
import br.com.portalmanager.platform.library.objectstorage.api.configuration.ObjectStorageProvider;
import br.com.portalmanager.platform.library.objectstorage.api.contract.ObjectStorageClient;
import br.com.portalmanager.platform.library.scheduling.task.PlatformTaskScheduler;
import org.springframework.scheduling.TaskScheduler;
import org.springframework.transaction.PlatformTransactionManager;
import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import tools.jackson.databind.ObjectMapper;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

class PlatformAuditAutoConfigurationTest {

    private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
            .withConfiguration(AutoConfigurations.of(PlatformAuditAutoConfiguration.class))
            .withUserConfiguration(TestInfrastructure.class);

    @Configuration
    static class TestInfrastructure {
        @Bean ObjectMapper objectMapper() { return new ObjectMapper(); }
        @Bean AuditOutboxAppender auditOutboxAppender() { return mock(AuditOutboxAppender.class); }
        @Bean AuditOutboxRepository<?> auditOutboxRepository() { return mock(AuditOutboxRepository.class); }
        @Bean ObjectStorageClient objectStorageClient() { return mock(ObjectStorageClient.class); }
        @Bean PlatformTransactionManager transactionManager() { return mock(PlatformTransactionManager.class); }
        @Bean PlatformTaskScheduler platformTaskScheduler() {
            TaskScheduler taskScheduler = mock(TaskScheduler.class);
            return new PlatformTaskScheduler(() -> taskScheduler);
        }
        @Bean ObjectStorageProperties objectStorageProperties() {
            ObjectStorageProperties properties = new ObjectStorageProperties();
            properties.setProvider(ObjectStorageProvider.AWS);
            properties.setRegion("us-east-1");
            ObjectStorageProperties.Destination destination = new ObjectStorageProperties.Destination();
            destination.setBucket("audit-test");
            properties.setDestinations(Map.of("audit", destination));
            return properties;
        }
    }

    @Test
    void shouldLoadTransactionalCaptureWhenEnabledAndConfigured() {
        contextRunner
                .withPropertyValues("platform.audit.enabled=true", "platform.audit.service-name=account",
                        "platform.audit.outbox.destination=audit")
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
    void shouldNotLoadAuditInfrastructureWhenDisabled() {
        contextRunner
                .withPropertyValues("platform.audit.enabled=false")
                .run(context -> {
                    assertThat(context).doesNotHaveBean(AuditAspect.class);
                    assertThat(context).doesNotHaveBean(PlatformAuditProperties.class);
                });
    }
}
