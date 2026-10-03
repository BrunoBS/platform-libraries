package br.com.portalmanager.platform.library.messagequeue.publisher;

import br.com.portalmanager.platform.library.messagequeue.contract.MessageQueuePublisher;
import br.com.portalmanager.platform.library.messagequeue.exception.MessagePublishException;
import br.com.portalmanager.platform.library.messagequeue.provider.MessageQueueTransport;
import br.com.portalmanager.platform.library.messagequeue.resolver.DestinationResolver;
import br.com.portalmanager.platform.library.messagequeue.serialization.MessageQueueSerializer;

import java.util.Map;

public class DefaultMessageQueuePublisher implements MessageQueuePublisher {

    private final DestinationResolver destinationResolver;
    private final MessageEnvelopeFactory envelopeFactory;
    private final MessageQueueSerializer serializer;
    private final MessageQueueTransport transport;

    public DefaultMessageQueuePublisher(
            DestinationResolver destinationResolver,
            MessageEnvelopeFactory envelopeFactory,
            MessageQueueSerializer serializer,
            MessageQueueTransport transport) {
        this.destinationResolver = destinationResolver;
        this.envelopeFactory = envelopeFactory;
        this.serializer = serializer;
        this.transport = transport;
    }

    @Override
    public void publish(String destination, Object payload) {
        publish(destination, payload, null, Map.of());
    }

    @Override
    public void publish(String destination, Object payload, String correlationId, Map<String, String> headers) {
        var resolved = destinationResolver.resolve(destination);
        if (!resolved.publisherEnabled()) {
            throw new MessagePublishException("Publisher is disabled for destination: " + destination);
        }

        var envelope = envelopeFactory.create(destination, payload, correlationId, headers);
        transport.send(resolved, serializer.serialize(envelope));
    }
}
