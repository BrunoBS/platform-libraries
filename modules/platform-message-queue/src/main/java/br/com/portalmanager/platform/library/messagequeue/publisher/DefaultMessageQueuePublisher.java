package br.com.portalmanager.platform.library.messagequeue.publisher;

import br.com.portalmanager.platform.library.messagequeue.contract.MessageQueuePublishOptions;
import br.com.portalmanager.platform.library.messagequeue.contract.MessageQueuePublisher;
import br.com.portalmanager.platform.library.messagequeue.exception.MessagePublishException;
import br.com.portalmanager.platform.library.messagequeue.provider.MessageQueueTransport;
import br.com.portalmanager.platform.library.messagequeue.monitoring.MessageQueueMetrics;
import br.com.portalmanager.platform.library.messagequeue.resolver.DestinationResolver;
import br.com.portalmanager.platform.library.messagequeue.serialization.MessageQueueSerializer;

import java.util.Map;

public class DefaultMessageQueuePublisher implements MessageQueuePublisher {

    private final DestinationResolver destinationResolver;
    private final MessageEnvelopeFactory envelopeFactory;
    private final MessageQueueSerializer serializer;
    private final MessageQueueTransport transport;
    private final MessageQueueMetrics metrics;

    public DefaultMessageQueuePublisher(
            DestinationResolver destinationResolver,
            MessageEnvelopeFactory envelopeFactory,
            MessageQueueSerializer serializer,
            MessageQueueTransport transport,
            MessageQueueMetrics metrics) {
        this.destinationResolver = destinationResolver;
        this.envelopeFactory = envelopeFactory;
        this.serializer = serializer;
        this.transport = transport;
        this.metrics = metrics;
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
        var resolved = destinationResolver.resolve(destination);
        try {
            if (!resolved.publisherEnabled()) {
                throw new MessagePublishException("Publisher is disabled for destination: " + destination);
            }

            var safeOptions = options == null ? MessageQueuePublishOptions.defaults() : options;
            var envelope = envelopeFactory.create(
                    destination, payload, safeOptions.correlationId(), safeOptions.headers());
            transport.send(resolved, serializer.serialize(envelope), safeOptions);
            metrics.recordPublish(resolved, true);
        } catch (RuntimeException exception) {
            metrics.recordPublish(resolved, false);
            throw exception;
        }
    }
}
