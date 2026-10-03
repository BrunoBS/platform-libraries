package br.com.portalmanager.platform.library.messagequeue.monitoring;

import br.com.portalmanager.platform.library.messagequeue.configuration.MessageQueueProvider;
import br.com.portalmanager.platform.library.messagequeue.resolver.ResolvedDestination;
import io.micrometer.core.instrument.Meter;
import io.micrometer.core.instrument.config.MeterFilter;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.jupiter.api.Test;

import java.time.Duration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;

class MessageQueueMetricsTest {

    @Test
    void shouldNotPropagateMetricRegistryFailuresToMessageProcessing() {
        var registry = new SimpleMeterRegistry();
        registry.config().meterFilter(new MeterFilter() {
            @Override
            public Meter.Id map(Meter.Id id) {
                throw new IllegalStateException("metrics unavailable");
            }
        });
        var metrics = new MessageQueueMetrics(registry);
        var destination = new ResolvedDestination(
                "orders",
                MessageQueueProvider.AWS,
                "physical-orders",
                "physical-orders-dlq",
                true,
                true,
                Duration.ofSeconds(30),
                Duration.ofSeconds(20),
                2);

        assertThatCode(() -> metrics.recordPublish(destination, true))
                .doesNotThrowAnyException();
    }

    @Test
    void shouldRecordPublishConsumeAndPollingOutcomesByProviderAndDestination() {
        var registry = new SimpleMeterRegistry();
        var metrics = new MessageQueueMetrics(registry);
        var destination = new ResolvedDestination(
                "orders",
                MessageQueueProvider.AWS,
                "physical-orders",
                "physical-orders-dlq",
                true,
                true,
                Duration.ofSeconds(30),
                Duration.ofSeconds(20),
                2);

        metrics.recordPublish(destination, true);
        metrics.recordConsume(MessageQueueProvider.AWS, "orders", false, true);
        metrics.recordConsume(MessageQueueProvider.AWS, "orders", true, false);
        metrics.recordPollFailure(MessageQueueProvider.AWS, "orders", false);
        metrics.recordAcknowledgementFailure(MessageQueueProvider.AWS, "orders", true);

        assertThat(registry.get("platform.message.queue.publish")
                .tag("provider", "AWS")
                .tag("destination", "orders")
                .tag("outcome", "success")
                .counter().count()).isEqualTo(1.0);
        assertThat(registry.get("platform.message.queue.consume")
                .tag("provider", "AWS")
                .tag("destination", "orders")
                .tag("kind", "dead-letter")
                .tag("outcome", "failure")
                .counter().count()).isEqualTo(1.0);
        assertThat(registry.get("platform.message.queue.ack.failure")
                .tag("provider", "AWS")
                .tag("destination", "orders")
                .tag("kind", "dead-letter")
                .counter().count()).isEqualTo(1.0);
        assertThat(registry.get("platform.message.queue.poll.failure")
                .tag("provider", "AWS")
                .tag("destination", "orders")
                .tag("kind", "queue")
                .counter().count()).isEqualTo(1.0);
    }
}
