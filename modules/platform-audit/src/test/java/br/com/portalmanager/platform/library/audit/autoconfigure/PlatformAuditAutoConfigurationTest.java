package br.com.portalmanager.platform.library.audit.autoconfigure;

import br.com.portalmanager.platform.library.audit.aspect.AuditAspect;
import br.com.portalmanager.platform.library.audit.context.AuditAuthorizationContextResolver;
import br.com.portalmanager.platform.library.audit.event.AuditEventFactory;
import br.com.portalmanager.platform.library.audit.field.AuditFieldResolver;
import br.com.portalmanager.platform.library.audit.publisher.AuditPublisher;
import br.com.portalmanager.platform.library.messagequeue.capability.QueueCapabilities;
import br.com.portalmanager.platform.library.messagequeue.capability.QueueCapabilitiesRegistry;
import br.com.portalmanager.platform.library.messagequeue.configuration.MessageQueueProperties;
import br.com.portalmanager.platform.library.messagequeue.configuration.MessageQueueProvider;
import br.com.portalmanager.platform.library.messagequeue.contract.MessageQueuePublisher;
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
        @Bean HttpServletRequest httpServletRequest() { return mock(HttpServletRequest.class); }
        @Bean ObjectMapper objectMapper() { return new ObjectMapper(); }
        @Bean MessageQueuePublisher messageQueuePublisher() { return mock(MessageQueuePublisher.class); }
        @Bean MessageQueueProperties messageQueueProperties() {
            MessageQueueProperties properties = new MessageQueueProperties();
            properties.setProvider(MessageQueueProvider.AWS);
            properties.getAws().setRegion("sa-east-1");
            MessageQueueProperties.Destination destination = new MessageQueueProperties.Destination();
            destination.setQueue("audit-events.fifo");
            properties.getDestinations().put("audit-events", destination);
            return properties;
        }
        @Bean QueueCapabilitiesRegistry queueCapabilitiesRegistry(MessageQueueProperties properties) {
            return new QueueCapabilitiesRegistry(
                    properties,
                    queue -> new QueueCapabilities(true, true));
        }
    }

    @Configuration
    static class CustomFactoryConfiguration {
        @Bean AuditEventFactory customAuditEventFactory() { return mock(AuditEventFactory.class); }
    }

    @Configuration
    static class CustomPublisherConfiguration {
        @Bean AuditPublisher customAuditPublisher() { return mock(AuditPublisher.class); }
    }

    @Test
    void shouldLoadAuditInfrastructureWithOrderedMessageQueueDestination() {
        contextRunner
                .withPropertyValues(
                        "platform.audit.enabled=true",
                        "platform.audit.service-name=account"
                )
                .run(context -> {
                    assertThat(context).hasSingleBean(AuditPublisher.class);
                    assertThat(context).hasSingleBean(AuditAuthorizationContextResolver.class);
                    assertThat(context).hasSingleBean(AuditFieldResolver.class);
                    assertThat(context).hasSingleBean(AuditEventFactory.class);
                    assertThat(context).hasSingleBean(AuditAspect.class);
                });
    }

    @Test
    void shouldFailStartupWhenAuditDestinationDoesNotExist() {
        contextRunner
                .withPropertyValues(
                        "platform.audit.enabled=true",
                        "platform.audit.service-name=account",
                        "platform.audit.destination=missing"
                )
                .run(context -> assertThat(context).hasFailed());
    }

    @Test
    void shouldRequireServiceNameEvenWithCustomPublisher() {
        contextRunner
                .withUserConfiguration(CustomPublisherConfiguration.class)
                .withPropertyValues("platform.audit.enabled=true")
                .run(context -> assertThat(context).hasFailed());
    }

    @Test
    void shouldRequireServiceNameEvenWithCustomFactory() {
        contextRunner
                .withUserConfiguration(CustomPublisherConfiguration.class, CustomFactoryConfiguration.class)
                .withPropertyValues("platform.audit.enabled=true")
                .run(context -> assertThat(context).hasFailed());
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
                    assertThat(context).doesNotHaveBean(AuditPublisher.class);
                    assertThat(context).doesNotHaveBean(AuditAuthorizationContextResolver.class);
                    assertThat(context).doesNotHaveBean(AuditAspect.class);
                });
    }
}
