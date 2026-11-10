package br.com.portalmanager.platform.library.messagequeue.internal.publisher;

import br.com.portalmanager.platform.library.messagequeue.api.contract.MessageQueuePublishOptions;
import br.com.portalmanager.platform.library.messagequeue.api.contract.MessageQueuePublisher;
import br.com.portalmanager.platform.library.messaging.api.exception.ApiException;
import br.com.portalmanager.platform.library.messaging.api.exception.ValidationException;
import br.com.portalmanager.platform.library.messagequeue.internal.message.MessageQueueMessageKeys;
import br.com.portalmanager.platform.library.messagequeue.internal.transport.MessageQueueTransport;
import br.com.portalmanager.platform.library.messagequeue.internal.resolver.DestinationResolver;
import br.com.portalmanager.platform.library.messagequeue.internal.serialization.MessageQueueSerializer;

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
        publish(destination, payload, MessageQueuePublishOptions.defaults());
    }

    @Override
    public void publish(String destination, Object payload, String correlationId, Map<String, String> headers) {
        publish(destination, payload, new MessageQueuePublishOptions(correlationId, headers, null, null));
    }

    @Override
    public void publish(String destination, Object payload, MessageQueuePublishOptions options) {
        if (payload == null) {
            throw new ValidationException(MessageQueueMessageKeys.PUBLISH_PAYLOAD_INVALID);
        }
        var resolved = destinationResolver.resolve(destination);
        if (!resolved.publisherEnabled()) {
            throw new ApiException(
                    MessageQueueMessageKeys.PUBLISHER_DISABLED,
                    Map.of("0", destination),
                    null);
        }

        var safeOptions = options == null ? MessageQueuePublishOptions.defaults() : options;
        var envelope = envelopeFactory.create(
                destination, payload, safeOptions.correlationId(), safeOptions.headers());
        transport.send(resolved, serializer.serialize(envelope), safeOptions);
    }
}
