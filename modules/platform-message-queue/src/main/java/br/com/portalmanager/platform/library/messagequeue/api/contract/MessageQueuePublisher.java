package br.com.portalmanager.platform.library.messagequeue.api.contract;

import br.com.portalmanager.platform.library.messaging.exception.ValidationException;

import java.util.Map;

public interface MessageQueuePublisher {

    void publish(String destination, Object payload);

    void publish(String destination, Object payload, String correlationId, Map<String, String> headers);

    default void publish(String destination, Object payload, MessageQueuePublishOptions options) {
        MessageQueuePublishOptions safeOptions = options == null ? MessageQueuePublishOptions.defaults() : options;
        if (safeOptions.orderingKey() != null || safeOptions.deduplicationId() != null) {
            throw new ValidationException("messagequeue.publish.options.unsupported");
        }
        publish(destination, payload, safeOptions.correlationId(), safeOptions.headers());
    }
}
