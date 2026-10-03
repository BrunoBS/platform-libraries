package br.com.portalmanager.platform.library.messagequeue.contract;

import java.time.Instant;
import java.util.Map;

public record DeadLetterMessage<T>(
        MessageQueueMessage<T> message,
        String reason,
        String description,
        Integer deliveryCount,
        Instant deadLetteredAt,
        Map<String, String> providerMetadata
) {
}
