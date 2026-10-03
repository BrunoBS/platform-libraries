package br.com.portalmanager.platform.library.messagequeue.contract;

import java.util.Map;

public interface MessageQueuePublisher {

    void publish(String destination, Object payload);

    void publish(String destination, Object payload, String correlationId, Map<String, String> headers);

    void publish(String destination, Object payload, MessageQueuePublishOptions options);
}
