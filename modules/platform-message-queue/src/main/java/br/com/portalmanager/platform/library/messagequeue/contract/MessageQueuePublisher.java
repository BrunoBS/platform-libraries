package br.com.portalmanager.platform.library.messagequeue.contract;

import br.com.portalmanager.platform.library.messagequeue.exception.MessagePublishException;
import br.com.portalmanager.platform.library.messagequeue.message.MessageQueueMessageKeys;

import java.util.Map;

public interface MessageQueuePublisher {

    void publish(String destination, Object payload);

    void publish(String destination, Object payload, String correlationId, Map<String, String> headers);

    default void publish(String destination, Object payload, MessageQueuePublishOptions options) {
        var safeOptions = options == null ? MessageQueuePublishOptions.defaults() : options;
        if (safeOptions.orderingKey() != null || safeOptions.deduplicationId() != null) {
            throw new MessagePublishException(MessageQueueMessageKeys.PUBLISH_OPTIONS_UNSUPPORTED, null);
        }
        publish(destination, payload, safeOptions.correlationId(), safeOptions.headers());
    }
}
