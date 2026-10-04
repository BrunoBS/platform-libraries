package br.com.portalmanager.platform.library.audit.publisher;

import br.com.portalmanager.platform.library.audit.exception.AuditException;
import br.com.portalmanager.platform.library.audit.message.AuditMessageKeys;
import br.com.portalmanager.platform.library.audit.model.AuditEventRequest;
import br.com.portalmanager.platform.library.messaging.exception.ApiException;
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

        try {
            messageQueuePublisher.publish(destination, event, options);
        } catch (ApiException exception) {
            throw exception;
        } catch (RuntimeException exception) {
            throw new AuditException(AuditMessageKeys.PUBLISH_FAILED, exception);
        }
    }

    private String orderingKey(String resourceType, String resourceIdentifier) {
        if (resourceType == null || resourceType.isBlank()
                || resourceIdentifier == null || resourceIdentifier.isBlank()) {
            throw new AuditException(AuditMessageKeys.RESOURCE_IDENTIFIER_MISSING);
        }

        String aggregateIdentity = resourceType + "\u0000" + resourceIdentifier;
        try {
            byte[] digest = MessageDigest.getInstance("SHA-256")
                    .digest(aggregateIdentity.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(digest);
        } catch (NoSuchAlgorithmException exception) {
            throw new AuditException(AuditMessageKeys.PUBLISH_FAILED, exception);
        }
    }
}
