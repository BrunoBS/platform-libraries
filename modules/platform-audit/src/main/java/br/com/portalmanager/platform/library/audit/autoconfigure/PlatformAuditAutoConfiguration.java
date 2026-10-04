package br.com.portalmanager.platform.library.audit.autoconfigure;

import br.com.portalmanager.platform.library.audit.aspect.AuditAspect;
import br.com.portalmanager.platform.library.audit.config.PlatformAuditProperties;
import br.com.portalmanager.platform.library.audit.context.AuditAuthorizationContextResolver;
import br.com.portalmanager.platform.library.audit.event.AuditEventFactory;
import br.com.portalmanager.platform.library.audit.field.AuditFieldResolver;
import br.com.portalmanager.platform.library.audit.message.AuditTechnicalErrors;
import br.com.portalmanager.platform.library.audit.publisher.AuditPublisher;
import br.com.portalmanager.platform.library.audit.publisher.MessageQueueAuditPublisher;
import br.com.portalmanager.platform.library.messaging.exception.PlatformConfigurationException;
import br.com.portalmanager.platform.library.messagequeue.configuration.MessageQueueProperties;
import br.com.portalmanager.platform.library.messagequeue.contract.MessageQueuePublisher;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import tools.jackson.databind.ObjectMapper;

@AutoConfiguration
@EnableConfigurationProperties(PlatformAuditProperties.class)
@ConditionalOnProperty(prefix = "platform.audit", name = "enabled", havingValue = "true", matchIfMissing = true)
public class PlatformAuditAutoConfiguration {

    @Bean
    @ConditionalOnMissingBean(AuditPublisher.class)
    AuditPublisher auditPublisher(
            MessageQueuePublisher messageQueuePublisher,
            PlatformAuditProperties auditProperties,
            MessageQueueProperties messageQueueProperties
    ) {
        String destinationName = auditProperties.getDestination();
        validateDestination(destinationName, messageQueueProperties);
        return new MessageQueueAuditPublisher(
                messageQueuePublisher,
                destinationName,
                messageQueueProperties.getProvider()
        );
    }

    @Bean
    @ConditionalOnMissingBean(AuditAuthorizationContextResolver.class)
    AuditAuthorizationContextResolver auditAuthorizationContextResolver() {
        return new AuditAuthorizationContextResolver();
    }

    @Bean
    @ConditionalOnMissingBean(AuditFieldResolver.class)
    AuditFieldResolver auditFieldResolver(
            PlatformAuditProperties properties,
            HttpServletRequest request
    ) {
        return new AuditFieldResolver(properties, request);
    }

    @Bean
    @ConditionalOnMissingBean(AuditEventFactory.class)
    AuditEventFactory auditEventFactory(
            PlatformAuditProperties properties,
            AuditAuthorizationContextResolver contextResolver,
            AuditFieldResolver fieldResolver,
            ObjectMapper objectMapper
    ) {
        validateProperties(properties);
        return new AuditEventFactory(properties, contextResolver, fieldResolver, objectMapper);
    }

    @Bean
    @ConditionalOnMissingBean(AuditAspect.class)
    AuditAspect auditAspect(
            PlatformAuditProperties properties,
            AuditEventFactory eventFactory,
            AuditPublisher publisher
    ) {
        return new AuditAspect(properties, eventFactory, publisher);
    }

    private void validateProperties(PlatformAuditProperties properties) {
        if (properties.getServiceName() == null || properties.getServiceName().isBlank()) {
            throw new PlatformConfigurationException(AuditTechnicalErrors.SERVICE_NAME_REQUIRED);
        }
        if (properties.getMaxEventSizeBytes() <= 0) {
            throw new PlatformConfigurationException(AuditTechnicalErrors.EVENT_SIZE_LIMIT_INVALID);
        }
        if (properties.getMaxEventsPerInvocation() <= 0) {
            throw new PlatformConfigurationException(AuditTechnicalErrors.EVENT_COUNT_LIMIT_INVALID);
        }
    }

    private void validateDestination(String destinationName, MessageQueueProperties properties) {
        if (destinationName == null || destinationName.isBlank()) {
            throw new PlatformConfigurationException(AuditTechnicalErrors.MESSAGE_QUEUE_DESTINATION_REQUIRED);
        }

        MessageQueueProperties.Destination destination = properties.getDestinations().get(destinationName);
        if (destination == null || !destination.isOrdered() || !destination.getPublisher().isEnabled()) {
            throw new PlatformConfigurationException(
                    AuditTechnicalErrors.MESSAGE_QUEUE_DESTINATION_NOT_CONFIGURED);
        }
    }
}
