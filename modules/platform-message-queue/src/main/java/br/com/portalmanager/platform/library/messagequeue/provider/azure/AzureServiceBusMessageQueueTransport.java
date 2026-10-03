package br.com.portalmanager.platform.library.messagequeue.provider.azure;

import br.com.portalmanager.platform.library.messagequeue.contract.MessageQueuePublishOptions;
import br.com.portalmanager.platform.library.messagequeue.exception.MessagePublishException;
import br.com.portalmanager.platform.library.messagequeue.provider.MessageQueueTransport;
import br.com.portalmanager.platform.library.messagequeue.resolver.ResolvedDestination;
import com.azure.messaging.servicebus.ServiceBusClientBuilder;
import com.azure.messaging.servicebus.ServiceBusMessage;
import com.azure.messaging.servicebus.ServiceBusSenderClient;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class AzureServiceBusMessageQueueTransport implements MessageQueueTransport, AutoCloseable {

    private final ServiceBusClientBuilder clientBuilder;
    private final Map<String, ServiceBusSenderClient> senders = new ConcurrentHashMap<>();

    public AzureServiceBusMessageQueueTransport(ServiceBusClientBuilder clientBuilder) {
        this.clientBuilder = clientBuilder;
    }

    @Override
    public void send(ResolvedDestination destination, String body) {
        send(destination, body, MessageQueuePublishOptions.defaults());
    }

    @Override
    public void send(ResolvedDestination destination, String body, MessageQueuePublishOptions options) {
        try {
            if (options.messageGroupId() != null || options.deduplicationId() != null) {
                throw new IllegalArgumentException("AWS FIFO publish options are not supported by Azure Service Bus");
            }
            sender(destination.queue()).sendMessage(new ServiceBusMessage(body));
        } catch (RuntimeException exception) {
            throw new MessagePublishException(
                    "Failed to publish message to destination: " + destination.logicalName(),
                    exception);
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
