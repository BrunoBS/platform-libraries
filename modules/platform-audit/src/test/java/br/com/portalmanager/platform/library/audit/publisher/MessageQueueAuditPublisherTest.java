package br.com.portalmanager.platform.library.audit.publisher;

import br.com.portalmanager.platform.library.audit.model.AuditEventRequest;
import br.com.portalmanager.platform.library.messagequeue.configuration.MessageQueueProvider;
import br.com.portalmanager.platform.library.messagequeue.contract.MessageQueuePublishOptions;
import br.com.portalmanager.platform.library.messagequeue.contract.MessageQueuePublisher;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;

class MessageQueueAuditPublisherTest {

    private final AuditEventRequest event = new AuditEventRequest(
            UUID.randomUUID().toString(), Instant.now(), "account", "account-1", "application-1", "dev",
            "account", "123", "UPDATE", "user", "correlation-1", 200,
            Map.of("id", "123"), Map.of());

    @Test
    void shouldPublishToLogicalAuditDestinationWithResourceOrderingAndSqsDeduplication() {
        MessageQueuePublisher queue = mock(MessageQueuePublisher.class);
        MessageQueueAuditPublisher publisher = new MessageQueueAuditPublisher(
                queue, "audit-events", MessageQueueProvider.AWS);

        publisher.publish(event);

        ArgumentCaptor<MessageQueuePublishOptions> options = ArgumentCaptor.forClass(MessageQueuePublishOptions.class);
        verify(queue).publish(org.mockito.ArgumentMatchers.eq("audit-events"),
                org.mockito.ArgumentMatchers.eq(event), options.capture());
        assertThat(options.getValue().correlationId()).isEqualTo(event.correlationId());
        assertThat(options.getValue().orderingKey()).hasSize(64);
        assertThat(options.getValue().deduplicationId()).isEqualTo(event.eventId());
    }

    @Test
    void shouldUseStableOrderingKeyForSameResourceAndOmitAwsDeduplicationForAzure() {
        MessageQueuePublisher queue = mock(MessageQueuePublisher.class);
        MessageQueueAuditPublisher publisher = new MessageQueueAuditPublisher(
                queue, "audit-events", MessageQueueProvider.AZURE);
        AuditEventRequest sameResource = new AuditEventRequest(
                UUID.randomUUID().toString(), Instant.now(), "account", "account-1", "application-1", "dev",
                "account", "123", "DELETE", "user", "correlation-2", 200,
                Map.of("id", "123"), Map.of());

        publisher.publish(event);
        publisher.publish(sameResource);

        ArgumentCaptor<MessageQueuePublishOptions> options = ArgumentCaptor.forClass(MessageQueuePublishOptions.class);
        verify(queue, org.mockito.Mockito.times(2)).publish(
                org.mockito.ArgumentMatchers.eq("audit-events"),
                org.mockito.ArgumentMatchers.any(AuditEventRequest.class), options.capture());
        assertThat(options.getAllValues().get(0).orderingKey())
                .isEqualTo(options.getAllValues().get(1).orderingKey());
        assertThat(options.getAllValues().get(0).deduplicationId()).isNull();
    }

    @Test
    void shouldPropagateBrokerFailure() {
        MessageQueuePublisher queue = mock(MessageQueuePublisher.class);
        doThrow(new IllegalStateException("broker unavailable")).when(queue).publish(
                org.mockito.ArgumentMatchers.anyString(),
                org.mockito.ArgumentMatchers.any(),
                org.mockito.ArgumentMatchers.any(MessageQueuePublishOptions.class));
        MessageQueueAuditPublisher publisher = new MessageQueueAuditPublisher(
                queue, "audit-events", MessageQueueProvider.AWS);

        assertThatThrownBy(() -> publisher.publish(event))
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("broker unavailable");
    }
}
