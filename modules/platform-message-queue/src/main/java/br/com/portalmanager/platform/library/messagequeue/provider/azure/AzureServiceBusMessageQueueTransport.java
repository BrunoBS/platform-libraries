package br.com.portalmanager.platform.library.messagequeue.provider.azure;

import br.com.portalmanager.platform.library.messagequeue.capability.QueueCapabilitiesRegistry;
import br.com.portalmanager.platform.library.messagequeue.contract.MessageQueuePublishOptions;
import br.com.portalmanager.platform.library.messagequeue.exception.MessagePublishException;
import br.com.portalmanager.platform.library.messagequeue.message.MessageQueueMessageKeys;
import br.com.portalmanager.platform.library.messagequeue.provider.MessageQueueTransport;
import br.com.portalmanager.platform.library.messagequeue.resolver.ResolvedDestination;
import com.azure.messaging.servicebus.ServiceBusClientBuilder;
import com.azure.messaging.servicebus.ServiceBusMessage;
import com.azure.messaging.servicebus.ServiceBusSenderClient;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class AzureServiceBusMessageQueueTransport implements MessageQueueTransport, AutoCloseable {

    private final ServiceBusClientBuilder clientBuilder;
    private final QueueCapabilitiesRegistry capabilitiesRegistry;
    private final Map<String, ServiceBusSenderClient> senders = new ConcurrentHashMap<>();

    public AzureServiceBusMessageQueueTransport(
            ServiceBusClientBuilder clientBuilder,
            QueueCapabilitiesRegistry capabilitiesRegistry) {
        this.clientBuilder = clientBuilder;
        this.capabilitiesRegistry = capabilitiesRegistry;
    }

    @Override
    public void send(ResolvedDestination destination, String body) {
        send(destination, body, MessageQueuePublishOptions.defaults());
    }

    @Override
    public void send(ResolvedDestination destination, String body, MessageQueuePublishOptions options) {
        try {
            if (options.deduplicationId() != null) {
                throw new IllegalArgumentException(
                        "deduplicationId is an AWS SQS FIFO option and is not supported by Azure Service Bus");
            }

            var capabilities = capabilitiesRegistry.get(destination);
            ServiceBusMessage message = new ServiceBusMessage(body);
            if (capabilities.ordered()) {
                if (options.orderingKey() == null || options.orderingKey().isBlank()) {
                    throw new IllegalArgumentException("orderingKey is required for an ordered destination");
                }
                validateOrderingKey(options.orderingKey());
                message.setSessionId(options.orderingKey());
            } else if (options.orderingKey() != null) {
                throw new IllegalArgumentException("orderingKey can only be used with an ordered destination");
            }

            sender(destination.queue()).sendMessage(message);
        } catch (RuntimeException exception) {
            if (!(exception instanceof IllegalArgumentException)) {
                capabilitiesRegistry.invalidate(destination.queue());
            }
            throw new MessagePublishException(
                    MessageQueueMessageKeys.PUBLISH_FAILED,
                    Map.of("0", destination.logicalName()),
                    exception);
        }
    }

    private void validateOrderingKey(String orderingKey) {
        if (orderingKey.length() > 128) {
            throw new IllegalArgumentException("orderingKey must contain between 1 and 128 characters");
        }
    }

    private ServiceBusSenderClient sender(String queueName) {
        return senders.computeIfAbsent(queueName, this::createSender);
    }

    private ServiceBusSenderClient createSender(String queueName) {
        return clientBuilder.sender().queueName(queueName).buildClient();
    }

    @Override
    public void close() {
        senders.values().forEach(ServiceBusSenderClient::close);
        senders.clear();
    }
}
