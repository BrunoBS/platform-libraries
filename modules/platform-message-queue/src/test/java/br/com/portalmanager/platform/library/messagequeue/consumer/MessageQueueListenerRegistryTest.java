package br.com.portalmanager.platform.library.messagequeue.consumer;

import br.com.portalmanager.platform.library.messagequeue.annotation.MessageQueueListener;
import br.com.portalmanager.platform.library.messagequeue.contract.MessageQueueMessage;
import org.junit.jupiter.api.Test;

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
        var message = new MessageQueueMessage<>(
                "message-id",
                "product.updated",
                "1",
                Instant.EPOCH,
                "correlation-id",
                Map.of(),
                new ProductUpdatedEvent("123"));

        var exception = assertThrows(IllegalStateException.class, () -> definition.invoke(message));

        assertEquals("business failure", exception.getMessage());
    }

    static class FailingListener {

        @MessageQueueListener("product-updated")
        public void consume(MessageQueueMessage<ProductUpdatedEvent> message) {
            throw new IllegalStateException("business failure");
        }
    }

    record ProductUpdatedEvent(String identifier) {
    }
}
