package br.com.portalmanager.platform.library.messagequeue.configuration;

import br.com.portalmanager.platform.library.messagequeue.consumer.MessageQueueListenerRegistry;
import br.com.portalmanager.platform.library.messagequeue.contract.MessageQueuePublisher;
import br.com.portalmanager.platform.library.messagequeue.provider.MessageQueueTransport;
import br.com.portalmanager.platform.library.messagequeue.provider.aws.SqsMessageQueueConsumer;
import br.com.portalmanager.platform.library.messagequeue.provider.aws.SqsMessageQueueTransport;
import br.com.portalmanager.platform.library.messagequeue.provider.azure.AzureServiceBusMessageQueueConsumer;
import br.com.portalmanager.platform.library.messagequeue.provider.azure.AzureServiceBusMessageQueueTransport;
import br.com.portalmanager.platform.library.messagequeue.monitoring.MessageQueueMetrics;
import br.com.portalmanager.platform.library.messagequeue.publisher.DefaultMessageQueuePublisher;
import br.com.portalmanager.platform.library.messagequeue.publisher.MessageEnvelopeFactory;
import br.com.portalmanager.platform.library.messagequeue.resolver.DestinationResolver;
import br.com.portalmanager.platform.library.messagequeue.serialization.MessageQueueSerializer;
import com.azure.identity.DefaultAzureCredentialBuilder;
import com.azure.messaging.servicebus.ServiceBusClientBuilder;
import io.micrometer.core.instrument.MeterRegistry;
import tools.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
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
    @ConditionalOnMissingBean
    MessageQueueMetrics messageQueueMetrics(ObjectProvider<MeterRegistry> meterRegistries) {
        return new MessageQueueMetrics(meterRegistries.getIfAvailable());
    }

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

    @Bean(destroyMethod = "close")
    @ConditionalOnMissingBean(SqsClient.class)
    @ConditionalOnProperty(prefix = "platform.message-queue", name = "provider", havingValue = "AWS")
    SqsClient messageQueueSqsClient(MessageQueueProperties properties) {
        return SqsClient.builder()
                .region(Region.of(properties.getAws().getRegion()))
                .build();
    }

    @Bean
    @ConditionalOnMissingBean(MessageQueueTransport.class)
    @ConditionalOnProperty(prefix = "platform.message-queue", name = "provider", havingValue = "AWS")
    MessageQueueTransport awsMessageQueueTransport(SqsClient messageQueueSqsClient) {
        return new SqsMessageQueueTransport(messageQueueSqsClient);
    }

    @Bean
    @ConditionalOnMissingBean(SqsMessageQueueConsumer.class)
    @ConditionalOnProperty(prefix = "platform.message-queue", name = "provider", havingValue = "AWS")
    SqsMessageQueueConsumer awsMessageQueueConsumer(
            SqsClient messageQueueSqsClient,
            MessageQueueListenerRegistry messageQueueListenerRegistry,
            DestinationResolver destinationResolver,
            MessageQueueSerializer messageQueueSerializer,
            MessageQueueProperties properties,
            MessageQueueMetrics messageQueueMetrics) {
        return new SqsMessageQueueConsumer(
                messageQueueSqsClient,
                messageQueueListenerRegistry,
                destinationResolver,
                messageQueueSerializer,
                properties,
                messageQueueMetrics);
    }

    @Bean
    @ConditionalOnMissingBean(ServiceBusClientBuilder.class)
    @ConditionalOnProperty(prefix = "platform.message-queue", name = "provider", havingValue = "AZURE")
    ServiceBusClientBuilder messageQueueAzureServiceBusClientBuilder(MessageQueueProperties properties) {
        return new ServiceBusClientBuilder()
                .credential(
                        properties.getAzure().getNamespace(),
                        new DefaultAzureCredentialBuilder().build());
    }

    @Bean(destroyMethod = "close")
    @ConditionalOnMissingBean(MessageQueueTransport.class)
    @ConditionalOnProperty(prefix = "platform.message-queue", name = "provider", havingValue = "AZURE")
    MessageQueueTransport azureMessageQueueTransport(ServiceBusClientBuilder messageQueueAzureServiceBusClientBuilder) {
        return new AzureServiceBusMessageQueueTransport(messageQueueAzureServiceBusClientBuilder);
    }

    @Bean
    @ConditionalOnMissingBean(AzureServiceBusMessageQueueConsumer.class)
    @ConditionalOnProperty(prefix = "platform.message-queue", name = "provider", havingValue = "AZURE")
    AzureServiceBusMessageQueueConsumer azureMessageQueueConsumer(
            ServiceBusClientBuilder messageQueueAzureServiceBusClientBuilder,
            MessageQueueListenerRegistry messageQueueListenerRegistry,
            DestinationResolver destinationResolver,
            MessageQueueSerializer messageQueueSerializer,
            MessageQueueProperties properties,
            MessageQueueMetrics messageQueueMetrics) {
        return new AzureServiceBusMessageQueueConsumer(
                messageQueueAzureServiceBusClientBuilder,
                messageQueueListenerRegistry,
                destinationResolver,
                messageQueueSerializer,
                properties,
                messageQueueMetrics);
    }

    @Bean
    @ConditionalOnMissingBean(MessageQueuePublisher.class)
    MessageQueuePublisher messageQueuePublisher(
            DestinationResolver destinationResolver,
            MessageEnvelopeFactory messageEnvelopeFactory,
            MessageQueueSerializer messageQueueSerializer,
            MessageQueueTransport messageQueueTransport,
            MessageQueueMetrics messageQueueMetrics) {
        return new DefaultMessageQueuePublisher(
                destinationResolver,
                messageEnvelopeFactory,
                messageQueueSerializer,
                messageQueueTransport,
                messageQueueMetrics);
    }
}
