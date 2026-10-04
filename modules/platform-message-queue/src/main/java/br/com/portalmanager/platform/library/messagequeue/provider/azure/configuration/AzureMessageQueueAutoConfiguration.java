package br.com.portalmanager.platform.library.messagequeue.provider.azure.configuration;

import br.com.portalmanager.platform.library.messagequeue.capability.QueueCapabilitiesRegistry;
import br.com.portalmanager.platform.library.messagequeue.capability.QueueCapabilitiesResolver;
import br.com.portalmanager.platform.library.messagequeue.configuration.MessageQueueProperties;
import br.com.portalmanager.platform.library.messagequeue.consumer.MessageQueueListenerRegistry;
import br.com.portalmanager.platform.library.messagequeue.provider.MessageQueueTransport;
import br.com.portalmanager.platform.library.messagequeue.provider.azure.AzureQueueCapabilitiesResolver;
import br.com.portalmanager.platform.library.messagequeue.provider.azure.AzureServiceBusMessageQueueConsumer;
import br.com.portalmanager.platform.library.messagequeue.provider.azure.AzureServiceBusMessageQueueTransport;
import br.com.portalmanager.platform.library.messagequeue.resolver.DestinationResolver;
import br.com.portalmanager.platform.library.messagequeue.serialization.MessageQueueSerializer;
import com.azure.identity.DefaultAzureCredentialBuilder;
import com.azure.messaging.servicebus.ServiceBusClientBuilder;
import com.azure.messaging.servicebus.administration.ServiceBusAdministrationClient;
import com.azure.messaging.servicebus.administration.ServiceBusAdministrationClientBuilder;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;

@AutoConfiguration
@ConditionalOnProperty(prefix = "platform.message-queue", name = "provider", havingValue = "AZURE")
public class AzureMessageQueueAutoConfiguration {

    @Bean
    @ConditionalOnMissingBean(ServiceBusClientBuilder.class)
    ServiceBusClientBuilder messageQueueAzureServiceBusClientBuilder(MessageQueueProperties properties) {
        return new ServiceBusClientBuilder()
                .credential(
                        properties.getAzure().getNamespace(),
                        new DefaultAzureCredentialBuilder().build());
    }

    @Bean
    @ConditionalOnMissingBean(ServiceBusAdministrationClient.class)
    ServiceBusAdministrationClient messageQueueAzureAdministrationClient(MessageQueueProperties properties) {
        return new ServiceBusAdministrationClientBuilder()
                .credential(
                        properties.getAzure().getNamespace(),
                        new DefaultAzureCredentialBuilder().build())
                .buildClient();
    }

    @Bean
    @ConditionalOnMissingBean(QueueCapabilitiesResolver.class)
    QueueCapabilitiesResolver azureQueueCapabilitiesResolver(
            ServiceBusAdministrationClient messageQueueAzureAdministrationClient) {
        return new AzureQueueCapabilitiesResolver(messageQueueAzureAdministrationClient);
    }

    @Bean(destroyMethod = "close")
    @ConditionalOnMissingBean(MessageQueueTransport.class)
    MessageQueueTransport azureMessageQueueTransport(
            ServiceBusClientBuilder messageQueueAzureServiceBusClientBuilder,
            QueueCapabilitiesRegistry capabilitiesRegistry) {
        return new AzureServiceBusMessageQueueTransport(
                messageQueueAzureServiceBusClientBuilder,
                capabilitiesRegistry);
    }

    @Bean
    @ConditionalOnMissingBean(AzureServiceBusMessageQueueConsumer.class)
    AzureServiceBusMessageQueueConsumer azureMessageQueueConsumer(
            ServiceBusClientBuilder messageQueueAzureServiceBusClientBuilder,
            MessageQueueListenerRegistry messageQueueListenerRegistry,
            DestinationResolver destinationResolver,
            MessageQueueSerializer messageQueueSerializer,
            MessageQueueProperties properties,
            QueueCapabilitiesRegistry capabilitiesRegistry) {
        return new AzureServiceBusMessageQueueConsumer(
                messageQueueAzureServiceBusClientBuilder,
                messageQueueListenerRegistry,
                destinationResolver,
                messageQueueSerializer,
                properties,
                capabilitiesRegistry);
    }
}
