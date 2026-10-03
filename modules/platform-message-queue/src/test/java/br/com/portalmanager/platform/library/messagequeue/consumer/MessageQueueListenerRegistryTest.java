package br.com.portalmanager.platform.library.messagequeue.consumer;

import br.com.portalmanager.platform.library.messagequeue.annotation.MessageQueueDeadLetterListener;
import br.com.portalmanager.platform.library.messagequeue.annotation.MessageQueueListener;
import br.com.portalmanager.platform.library.messagequeue.contract.DeadLetterMessage;
import br.com.portalmanager.platform.library.messagequeue.contract.MessageQueueMessage;
import org.junit.jupiter.api.Test;
import org.springframework.aop.framework.ProxyFactory;

import java.time.Instant;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class MessageQueueListenerRegistryTest {

    @Test
    void shouldPreserveBusinessRuntimeException() {
        var registry = new MessageQueueListenerRegistry();
        registry.postProcessAfterInitialization(new FailingListener(), "failingListener");

        var definition = registry.listeners().iterator().next();
        var message = message();

        var exception = assertThrows(IllegalStateException.class, () -> definition.invoke(message));

        assertEquals("business failure", exception.getMessage());
    }

    @Test
    void shouldDiscoverAndInvokeListenerThroughJdkProxy() {
        var target = new JdkProxyListener();
        var proxyFactory = new ProxyFactory(target);
        proxyFactory.setInterfaces(ProductListener.class);
        proxyFactory.setProxyTargetClass(false);
        Object proxy = proxyFactory.getProxy();

        var registry = new MessageQueueListenerRegistry();
        registry.postProcessAfterInitialization(proxy, "jdkProxyListener");
        var message = message();

        registry.listeners().iterator().next().invoke(message);

        assertEquals(message, target.received);
    }

    @Test
    void shouldRegisterDeadLetterListenerWithConcretePayloadType() {
        var registry = new MessageQueueListenerRegistry();
        registry.postProcessAfterInitialization(new DeadLetterListener(), "deadLetterListener");

        var definition = registry.deadLetterListeners().iterator().next();

        assertEquals("product-updated", definition.destination());
        assertEquals(ProductUpdatedEvent.class, definition.payloadType());
    }

    @Test
    void shouldPreserveDeadLetterBusinessRuntimeException() {
        var registry = new MessageQueueListenerRegistry();
        registry.postProcessAfterInitialization(new FailingDeadLetterListener(), "failingDeadLetterListener");

        var definition = registry.deadLetterListeners().iterator().next();
        var deadLetter = new DeadLetterMessage<>(message(), null, null, null, null, Map.of());

        var exception = assertThrows(IllegalStateException.class, () -> definition.invoke(deadLetter));

        assertEquals("dead-letter business failure", exception.getMessage());
    }

    private MessageQueueMessage<ProductUpdatedEvent> message() {
        return new MessageQueueMessage<>(
                "message-id",
                "product.updated",
                "1",
                Instant.EPOCH,
                "correlation-id",
                Map.of(),
                new ProductUpdatedEvent("123"));
    }

    interface ProductListener {
        void consume(MessageQueueMessage<ProductUpdatedEvent> message);
    }

    static class JdkProxyListener implements ProductListener {
        private MessageQueueMessage<ProductUpdatedEvent> received;

        @Override
        @MessageQueueListener("product-updated")
        public void consume(MessageQueueMessage<ProductUpdatedEvent> message) {
            received = message;
        }
    }

    static class FailingListener {

        @MessageQueueListener("product-updated")
        public void consume(MessageQueueMessage<ProductUpdatedEvent> message) {
            throw new IllegalStateException("business failure");
        }
    }

    static class DeadLetterListener {

        @MessageQueueDeadLetterListener("product-updated")
        public void consume(DeadLetterMessage<ProductUpdatedEvent> message) {
        }
    }

    static class FailingDeadLetterListener {

        @MessageQueueDeadLetterListener("product-updated")
        public void consume(DeadLetterMessage<ProductUpdatedEvent> message) {
            throw new IllegalStateException("dead-letter business failure");
        }
    }

    record ProductUpdatedEvent(String identifier) {
    }
}
