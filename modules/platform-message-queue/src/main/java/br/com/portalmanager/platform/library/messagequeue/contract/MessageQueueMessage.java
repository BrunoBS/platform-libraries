package br.com.portalmanager.platform.library.messagequeue.contract;

import java.time.Instant;
import java.util.Map;

public record MessageQueueMessage<T>(
        String messageId,
        String messageType,
        String messageVersion,
        Instant timestamp,
        String correlationId,
        Map<String, String> headers,
        T payload
) {
}
