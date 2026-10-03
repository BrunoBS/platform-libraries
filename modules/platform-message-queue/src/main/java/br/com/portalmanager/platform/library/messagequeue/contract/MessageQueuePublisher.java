package br.com.portalmanager.platform.library.messagequeue.contract;

import java.util.Map;

public interface MessageQueuePublisher {

    void publish(String destination, Object payload);

    void publish(String destination, Object payload, String correlationId, Map<String, String> headers);

    default void publish(String destination, Object payload, MessageQueuePublishOptions options) {
        var safeOptions = options == null ? MessageQueuePublishOptions.defaults() : options;
        if (safeOptions.orderingKey() != null || safeOptions.deduplicationId() != null) {
            throw new UnsupportedOperationException("This publisher does not support ordered publish options");
        }
        publish(destination, payload, safeOptions.correlationId(), safeOptions.headers());
    }
}
