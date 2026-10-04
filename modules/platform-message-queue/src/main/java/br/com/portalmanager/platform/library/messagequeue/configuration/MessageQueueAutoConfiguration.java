package br.com.portalmanager.platform.library.messagequeue.configuration;

import br.com.portalmanager.platform.library.messagequeue.capability.QueueCapabilitiesRegistry;
import br.com.portalmanager.platform.library.messagequeue.capability.QueueCapabilitiesResolver;
import br.com.portalmanager.platform.library.messagequeue.consumer.MessageQueueListenerRegistry;
import br.com.portalmanager.platform.library.messagequeue.contract.MessageQueuePublisher;
import br.com.portalmanager.platform.library.messagequeue.provider.MessageQueueTransport;
import br.com.portalmanager.platform.library.messagequeue.publisher.DefaultMessageQueuePublisher;
import br.com.portalmanager.platform.library.messagequeue.publisher.MessageEnvelopeFactory;
import br.com.portalmanager.platform.library.messagequeue.resolver.DestinationResolver;
import br.com.portalmanager.platform.library.messagequeue.serialization.MessageQueueSerializer;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import tools.jackson.databind.ObjectMapper;

import java.time.Clock;

@AutoConfiguration
@EnableConfigurationProperties(MessageQueueProperties.class)
public class MessageQueueAutoConfiguration {

    @Bean
    @ConditionalOnMissingBean(Clock.class)
    Clock messageQueueClock() {
        return Clock.systemUTC();
    }

    @Bean
    @ConditionalOnMissingBean
    DestinationResolver destinationResolver(MessageQueueProperties properties) {
        return new DestinationResolver(properties);
    }

    @Bean
    @ConditionalOnMissingBean
    MessageEnvelopeFactory messageEnvelopeFactory(Clock messageQueueClock) {
        return new MessageEnvelopeFactory(messageQueueClock);
    }

    @Bean
    @ConditionalOnMissingBean
    MessageQueueSerializer messageQueueSerializer(ObjectMapper objectMapper) {
        return new MessageQueueSerializer(objectMapper);
    }

    @Bean
    @ConditionalOnMissingBean
    static MessageQueueListenerRegistry messageQueueListenerRegistry() {
        return new MessageQueueListenerRegistry();
    }

    @Bean
    @ConditionalOnMissingBean
    QueueCapabilitiesRegistry queueCapabilitiesRegistry(
            MessageQueueProperties properties,
            QueueCapabilitiesResolver capabilitiesResolver) {
        return new QueueCapabilitiesRegistry(properties, capabilitiesResolver);
    }

    @Bean
    @ConditionalOnMissingBean(MessageQueuePublisher.class)
    MessageQueuePublisher messageQueuePublisher(
            DestinationResolver destinationResolver,
            MessageEnvelopeFactory messageEnvelopeFactory,
            MessageQueueSerializer messageQueueSerializer,
            MessageQueueTransport messageQueueTransport) {
        return new DefaultMessageQueuePublisher(
                destinationResolver,
                messageEnvelopeFactory,
                messageQueueSerializer,
                messageQueueTransport);
    }
}
