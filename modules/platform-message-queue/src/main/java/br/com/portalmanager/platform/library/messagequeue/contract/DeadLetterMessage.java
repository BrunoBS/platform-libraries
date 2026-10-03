package br.com.portalmanager.platform.library.messagequeue.contract;

import java.time.Instant;
import java.util.Map;

/**
 * Message delivered to a dead-letter handler.
 *
 * <p>Broker-specific metadata may be unavailable and is represented by {@code null}.
 * {@code deliveryCount} is the count reported by the provider when the dead-letter
 * handler receives the message; its meaning is provider-specific and is not guaranteed
 * to equal the number of failures on the source queue.</p>
 */
public record DeadLetterMessage<T>(
        MessageQueueMessage<T> message,
        String reason,
        String description,
        Integer deliveryCount,
        Instant deadLetteredAt,
        Map<String, String> providerMetadata
) {
}
