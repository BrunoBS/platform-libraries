package br.com.portalmanager.platform.library.audit.publisher;

import br.com.portalmanager.platform.library.audit.model.AuditEventRequest;
import br.com.portalmanager.platform.library.messagequeue.configuration.MessageQueueProvider;
import br.com.portalmanager.platform.library.messagequeue.contract.MessageQueuePublishOptions;
import br.com.portalmanager.platform.library.messagequeue.contract.MessageQueuePublisher;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import java.util.Map;
import java.util.Objects;

public final class MessageQueueAuditPublisher implements AuditPublisher {

    private final MessageQueuePublisher messageQueuePublisher;
    private final String destination;
    private final MessageQueueProvider provider;

    public MessageQueueAuditPublisher(
            MessageQueuePublisher messageQueuePublisher,
            String destination,
            MessageQueueProvider provider
    ) {
        this.messageQueuePublisher = Objects.requireNonNull(messageQueuePublisher);
        this.destination = Objects.requireNonNull(destination);
        this.provider = Objects.requireNonNull(provider);
    }

    @Override
    public void publish(AuditEventRequest event) {
        String orderingKey = orderingKey(event.resource(), event.resourceId());
        String deduplicationId = provider == MessageQueueProvider.AWS ? event.eventId() : null;
        MessageQueuePublishOptions options = new MessageQueuePublishOptions(
                event.correlationId(),
                Map.of(),
                orderingKey,
                deduplicationId
        );

        messageQueuePublisher.publish(destination, event, options);
    }

    private String orderingKey(String resourceType, String resourceIdentifier) {
        if (resourceType == null || resourceType.isBlank()
                || resourceIdentifier == null || resourceIdentifier.isBlank()) {
            throw new IllegalArgumentException("Audit resource type and identifier are required for ordered publication");
        }

        String aggregateIdentity = resourceType + "\u0000" + resourceIdentifier;
        try {
            byte[] digest = MessageDigest.getInstance("SHA-256")
                    .digest(aggregateIdentity.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(digest);
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException("SHA-256 is unavailable", exception);
        }
    }
}
