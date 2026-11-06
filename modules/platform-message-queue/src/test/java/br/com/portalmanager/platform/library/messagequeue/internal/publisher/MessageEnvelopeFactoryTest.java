package br.com.portalmanager.platform.library.messagequeue.internal.publisher;

import br.com.portalmanager.platform.library.messagequeue.api.annotation.QueueMessage;
import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class MessageEnvelopeFactoryTest {

    private static final Instant NOW = Instant.parse("2026-10-03T14:30:00Z");

    @Test
    void shouldUseDestinationAndVersionOneByConvention() {
        var factory = new MessageEnvelopeFactory(Clock.fixed(NOW, ZoneOffset.UTC));

        var message = factory.create("product-updated", new PlainEvent("123"), "corr-1", Map.of());

        assertThat(message.messageId()).isNotBlank();
        assertThat(message.messageType()).isEqualTo("product-updated");
        assertThat(message.messageVersion()).isEqualTo("1");
        assertThat(message.timestamp()).isEqualTo(NOW);
        assertThat(message.correlationId()).isEqualTo("corr-1");
    }

    @Test
    void shouldUseQueueMessageMetadataWhenPresent() {
        var factory = new MessageEnvelopeFactory(Clock.fixed(NOW, ZoneOffset.UTC));

        var message = factory.create("product-updated", new VersionedEvent("123"), null, null);

        assertThat(message.messageType()).isEqualTo("product.updated");
        assertThat(message.messageVersion()).isEqualTo("2");
        assertThat(message.headers()).isEmpty();
    }

    @Test
    void shouldApplyTheDocumentedDefaultsWhenQueueMessageMetadataIsBlank() {
        var factory = new MessageEnvelopeFactory(Clock.fixed(NOW, ZoneOffset.UTC));

        var message = factory.create("orders", new DefaultMetadataEvent("123"), null, Map.of());

        assertThat(message.messageType()).isEqualTo("orders");
        assertThat(message.messageVersion()).isEqualTo("1");
    }

    record PlainEvent(String identifier) {
    }

    @QueueMessage(type = "product.updated", version = "2")
    record VersionedEvent(String identifier) {
    }

    @QueueMessage(type = "", version = "")
    record DefaultMetadataEvent(String identifier) {
    }
}
