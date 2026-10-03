package br.com.portalmanager.platform.library.messagequeue.contract;

import java.util.Map;

public interface MessageQueuePublisher {

    void publish(String destination, Object payload);

    default void publish(String destination, Object payload, String correlationId, Map<String, String> headers) {
        publish(destination, payload);
    }
}
