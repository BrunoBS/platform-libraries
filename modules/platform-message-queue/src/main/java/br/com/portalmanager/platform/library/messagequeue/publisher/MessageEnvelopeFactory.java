package br.com.portalmanager.platform.library.messagequeue.publisher;

import br.com.portalmanager.platform.library.messagequeue.contract.MessageQueueMessage;

import java.time.Clock;
import java.time.Instant;
import java.util.Map;
import java.util.UUID;

public class MessageEnvelopeFactory {

    private final Clock clock;
    private final MessageMetadataResolver metadataResolver;

    public MessageEnvelopeFactory(Clock clock) {
        this.clock = clock;
        this.metadataResolver = new MessageMetadataResolver();
    }

    public MessageQueueMessage<Object> create(
            String destination,
            Object payload,
            String correlationId,
            Map<String, String> headers) {
        var metadata = metadataResolver.resolve(destination, payload);
        return new MessageQueueMessage<>(
                UUID.randomUUID().toString(),
                metadata.type(),
                metadata.version(),
                Instant.now(clock),
                correlationId,
                headers == null ? Map.of() : Map.copyOf(headers),
                payload
        );
    }
}
