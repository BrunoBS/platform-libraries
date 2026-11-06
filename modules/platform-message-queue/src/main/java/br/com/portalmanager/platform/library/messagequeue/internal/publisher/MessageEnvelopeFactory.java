package br.com.portalmanager.platform.library.messagequeue.internal.publisher;

import br.com.portalmanager.platform.library.messagequeue.api.contract.MessageQueueMessage;
import br.com.portalmanager.platform.library.messagequeue.internal.message.MessageMetadataResolver;

import java.time.Clock;
import java.time.Instant;
import java.util.Map;
import java.util.UUID;

public class MessageEnvelopeFactory {

    private final Clock clock;

    public MessageEnvelopeFactory(Clock clock) {
        this.clock = clock;
    }

    public MessageQueueMessage<Object> create(
            String destination,
            Object payload,
            String correlationId,
            Map<String, String> headers) {
        var metadata = MessageMetadataResolver.resolve(destination, payload.getClass());
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
