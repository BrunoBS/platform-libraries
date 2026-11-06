package br.com.portalmanager.platform.library.messagequeue.internal.consumer;

import br.com.portalmanager.platform.library.messagequeue.api.contract.MessageQueueMessage;
import org.junit.jupiter.api.Test;
import org.slf4j.MDC;

import java.time.Instant;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class MessageQueueMdcContextTest {

    @Test
    void installsMessageIdentifiersAndRestoresPreviousContext() {
        MDC.put("correlationId", "outer-correlation");
        MDC.put("accountId", "outer-account");
        var message = new MessageQueueMessage<>(
                "message-1", "OrderCreated", "1", Instant.EPOCH,
                "correlation-1", Map.of(), Map.of("orderId", "o-1"));

        MessageQueueMdcContext.run(message, () -> {
            assertThat(MDC.get("correlationId")).isEqualTo("correlation-1");
            assertThat(MDC.get("messageId")).isEqualTo("message-1");
            assertThat(MDC.get("accountId")).isNull();
        });

        assertThat(MDC.get("correlationId")).isEqualTo("outer-correlation");
        assertThat(MDC.get("messageId")).isNull();
        assertThat(MDC.get("accountId")).isEqualTo("outer-account");
        MDC.clear();
    }
}
