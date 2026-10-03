package br.com.portalmanager.platform.library.messagequeue.contract;

public interface MessageQueuePublisher {

    void publish(String destination, Object payload);
}
