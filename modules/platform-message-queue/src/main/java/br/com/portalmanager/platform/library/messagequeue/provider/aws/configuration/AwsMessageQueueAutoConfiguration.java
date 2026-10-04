package br.com.portalmanager.platform.library.messagequeue.provider.aws.configuration;

import br.com.portalmanager.platform.library.messagequeue.capability.QueueCapabilitiesRegistry;
import br.com.portalmanager.platform.library.messagequeue.capability.QueueCapabilitiesResolver;
import br.com.portalmanager.platform.library.messagequeue.configuration.MessageQueueProperties;
import br.com.portalmanager.platform.library.messagequeue.consumer.MessageQueueListenerRegistry;
import br.com.portalmanager.platform.library.messagequeue.provider.MessageQueueTransport;
import br.com.portalmanager.platform.library.messagequeue.provider.aws.AwsQueueCapabilitiesResolver;
import br.com.portalmanager.platform.library.messagequeue.provider.aws.SqsMessageQueueConsumer;
import br.com.portalmanager.platform.library.messagequeue.provider.aws.SqsMessageQueueTransport;
import br.com.portalmanager.platform.library.messagequeue.resolver.DestinationResolver;
import br.com.portalmanager.platform.library.messagequeue.serialization.MessageQueueSerializer;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.sqs.SqsClient;

@AutoConfiguration
@ConditionalOnProperty(prefix = "platform.message-queue", name = "provider", havingValue = "AWS")
public class AwsMessageQueueAutoConfiguration {

    @Bean(destroyMethod = "close")
    @ConditionalOnMissingBean(SqsClient.class)
    SqsClient messageQueueSqsClient(MessageQueueProperties properties) {
        return SqsClient.builder()
                .region(Region.of(properties.getAws().getRegion()))
                .build();
    }

    @Bean
    @ConditionalOnMissingBean(QueueCapabilitiesResolver.class)
    QueueCapabilitiesResolver awsQueueCapabilitiesResolver(SqsClient messageQueueSqsClient) {
        return new AwsQueueCapabilitiesResolver(messageQueueSqsClient);
    }

    @Bean
    @ConditionalOnMissingBean(MessageQueueTransport.class)
    MessageQueueTransport awsMessageQueueTransport(
            SqsClient messageQueueSqsClient,
            QueueCapabilitiesRegistry capabilitiesRegistry) {
        return new SqsMessageQueueTransport(messageQueueSqsClient, capabilitiesRegistry);
    }

    @Bean
    @ConditionalOnMissingBean(SqsMessageQueueConsumer.class)
    SqsMessageQueueConsumer awsMessageQueueConsumer(
            SqsClient messageQueueSqsClient,
            MessageQueueListenerRegistry messageQueueListenerRegistry,
            DestinationResolver destinationResolver,
            MessageQueueSerializer messageQueueSerializer,
            MessageQueueProperties properties,
            QueueCapabilitiesRegistry capabilitiesRegistry) {
        return new SqsMessageQueueConsumer(
                messageQueueSqsClient,
                messageQueueListenerRegistry,
                destinationResolver,
                messageQueueSerializer,
                properties,
                capabilitiesRegistry);
    }
}
