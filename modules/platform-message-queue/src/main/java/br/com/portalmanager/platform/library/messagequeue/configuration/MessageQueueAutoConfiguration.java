package br.com.portalmanager.platform.library.messagequeue.configuration;

import br.com.portalmanager.platform.library.messagequeue.consumer.MessageQueueListenerRegistry;
import br.com.portalmanager.platform.library.messagequeue.contract.MessageQueuePublisher;
import br.com.portalmanager.platform.library.messagequeue.provider.MessageQueueTransport;
import br.com.portalmanager.platform.library.messagequeue.provider.aws.SqsMessageQueueConsumer;
import br.com.portalmanager.platform.library.messagequeue.provider.aws.SqsMessageQueueTransport;
import br.com.portalmanager.platform.library.messagequeue.provider.azure.AzureServiceBusMessageQueueConsumer;
import br.com.portalmanager.platform.library.messagequeue.provider.azure.AzureServiceBusMessageQueueTransport;
import br.com.portalmanager.platform.library.messagequeue.publisher.DefaultMessageQueuePublisher;
import br.com.portalmanager.platform.library.messagequeue.publisher.MessageEnvelopeFactory;
import br.com.portalmanager.platform.library.messagequeue.resolver.DestinationResolver;
import br.com.portalmanager.platform.library.messagequeue.serialization.MessageQueueSerializer;
import com.azure.identity.DefaultAzureCredentialBuilder;
import com.azure.messaging.servicebus.ServiceBusClientBuilder;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.sqs.SqsClient;

import java.time.Clock;

@AutoConfiguration
@EnableConfigurationProperties(MessageQueueProperties.class)
public class MessageQueueAutoConfiguration {

    @Bean
    Clock messageQueueClock() {
        return Clock.systemUTC();
    }

    @Bean
    DestinationResolver destinationResolver(MessageQueueProperties properties) {
        return new DestinationResolver(properties);
    }

    @Bean
    MessageEnvelopeFactory messageEnvelopeFactory(Clock messageQueueClock) {
        return new MessageEnvelopeFactory(messageQueueClock);
    }

    @Bean
    MessageQueueSerializer messageQueueSerializer(ObjectMapper objectMapper) {
        return new MessageQueueSerializer(objectMapper);
    }

    @Bean
    MessageQueueListenerRegistry messageQueueListenerRegistry() {
        return new MessageQueueListenerRegistry();
    }

    @Bean(destroyMethod = "close")
    @ConditionalOnProperty(prefix = "platform.message-queue", name = "provider", havingValue = "AWS")
    SqsClient messageQueueSqsClient(MessageQueueProperties properties) {
        return SqsClient.builder()
                .region(Region.of(properties.getAws().getRegion()))
                .build();
    }

    @Bean
    @ConditionalOnProperty(prefix = "platform.message-queue", name = "provider", havingValue = "AWS")
    MessageQueueTransport awsMessageQueueTransport(SqsClient messageQueueSqsClient) {
        return new SqsMessageQueueTransport(messageQueueSqsClient);
    }

    @Bean
    @ConditionalOnProperty(prefix = "platform.message-queue", name = "provider", havingValue = "AWS")
    SqsMessageQueueConsumer awsMessageQueueConsumer(
            SqsClient messageQueueSqsClient,
            MessageQueueListenerRegistry messageQueueListenerRegistry,
            DestinationResolver destinationResolver,
            MessageQueueSerializer messageQueueSerializer,
            MessageQueueProperties properties) {
        return new SqsMessageQueueConsumer(
                messageQueueSqsClient,
                messageQueueListenerRegistry,
                destinationResolver,
                messageQueueSerializer,
                properties);
    }

    @Bean
    @ConditionalOnProperty(prefix = "platform.message-queue", name = "provider", havingValue = "AZURE")
    ServiceBusClientBuilder messageQueueAzureServiceBusClientBuilder(MessageQueueProperties properties) {
        return new ServiceBusClientBuilder()
                .credential(
                        properties.getAzure().getNamespace(),
                        new DefaultAzureCredentialBuilder().build());
    }

    @Bean(destroyMethod = "close")
    @ConditionalOnProperty(prefix = "platform.message-queue", name = "provider", havingValue = "AZURE")
    MessageQueueTransport azureMessageQueueTransport(ServiceBusClientBuilder messageQueueAzureServiceBusClientBuilder) {
        return new AzureServiceBusMessageQueueTransport(messageQueueAzureServiceBusClientBuilder);
    }

    @Bean
    @ConditionalOnProperty(prefix = "platform.message-queue", name = "provider", havingValue = "AZURE")
    AzureServiceBusMessageQueueConsumer azureMessageQueueConsumer(
            ServiceBusClientBuilder messageQueueAzureServiceBusClientBuilder,
            MessageQueueListenerRegistry messageQueueListenerRegistry,
            DestinationResolver destinationResolver,
            MessageQueueSerializer messageQueueSerializer,
            MessageQueueProperties properties) {
        return new AzureServiceBusMessageQueueConsumer(
                messageQueueAzureServiceBusClientBuilder,
                messageQueueListenerRegistry,
                destinationResolver,
                messageQueueSerializer,
                properties);
    }

    @Bean
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
