package br.com.portalmanager.platform.library.messagequeue.contract;

import java.util.Map;

/**
 * Optional metadata used when publishing a message.
 * Provider-neutral message metadata. {@code orderingKey} maps to the provider's
 * per-group ordering field (SQS MessageGroupId or Service Bus SessionId).
 * Deduplication remains provider-specific and is currently supported by AWS SQS FIFO.
 */
public record MessageQueuePublishOptions(
        String correlationId,
        Map<String, String> headers,
        String orderingKey,
        String deduplicationId) {

    public MessageQueuePublishOptions {
        headers = headers == null ? Map.of() : Map.copyOf(headers);
    }

    public static MessageQueuePublishOptions defaults() {
        return new MessageQueuePublishOptions(null, Map.of(), null, null);
    }

}
