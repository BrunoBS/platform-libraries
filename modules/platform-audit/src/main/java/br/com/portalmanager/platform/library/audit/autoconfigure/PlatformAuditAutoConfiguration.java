package br.com.portalmanager.platform.library.audit.autoconfigure;

import br.com.portalmanager.platform.library.audit.aspect.AuditAspect;
import br.com.portalmanager.platform.library.audit.config.PlatformAuditProperties;
import br.com.portalmanager.platform.library.audit.context.AuditAuthorizationContextResolver;
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
        if (destinationName == null || destinationName.isBlank()) {
            throw new PlatformConfigurationException(AuditTechnicalErrors.MESSAGE_QUEUE_DESTINATION_REQUIRED);
        }

        MessageQueueProperties.Destination destination =
                messageQueueProperties.getDestinations().get(destinationName);
        if (destination == null || !destination.isOrdered() || !destination.getPublisher().isEnabled()) {
            throw new PlatformConfigurationException(
                    AuditTechnicalErrors.MESSAGE_QUEUE_DESTINATION_NOT_CONFIGURED);
        }

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
    @ConditionalOnMissingBean(AuditAspect.class)
    AuditAspect auditAspect(
            PlatformAuditProperties properties,
            AuditPublisher publisher,
            AuditAuthorizationContextResolver contextResolver,
            ObjectMapper objectMapper,
            HttpServletRequest request
    ) {
        return new AuditAspect(properties, publisher, contextResolver, objectMapper, request);
    }
}
