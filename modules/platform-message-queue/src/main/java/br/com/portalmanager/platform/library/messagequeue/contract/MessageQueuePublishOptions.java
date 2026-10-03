package br.com.portalmanager.platform.library.messagequeue.contract;

import java.util.Map;

/**
 * Optional metadata used when publishing a message.
 * FIFO routing fields are supported by AWS SQS FIFO destinations.
 */
public record MessageQueuePublishOptions(
        String correlationId,
        Map<String, String> headers,
        String messageGroupId,
        String deduplicationId) {

    public MessageQueuePublishOptions {
        headers = headers == null ? Map.of() : Map.copyOf(headers);
    }

    public static MessageQueuePublishOptions defaults() {
        return new MessageQueuePublishOptions(null, Map.of(), null, null);
    }
}
