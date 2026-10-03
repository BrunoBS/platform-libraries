package br.com.portalmanager.platform.library.messagequeue.monitoring;

import br.com.portalmanager.platform.library.messagequeue.configuration.MessageQueueProvider;
import br.com.portalmanager.platform.library.messagequeue.resolver.ResolvedDestination;
import io.micrometer.core.instrument.MeterRegistry;

public final class MessageQueueMetrics {

    private final MeterRegistry meterRegistry;

    public MessageQueueMetrics(MeterRegistry meterRegistry) {
        this.meterRegistry = meterRegistry;
    }

    public void recordPublish(ResolvedDestination destination, boolean success) {
        increment(
                "platform.message.queue.publish",
                "provider", destination.provider().name(),
                "destination", destination.logicalName(),
                "outcome", outcome(success));
    }

    public void recordConsume(
            MessageQueueProvider provider,
            String destination,
            boolean deadLetter,
            boolean success) {
        increment(
                "platform.message.queue.consume",
                "provider", provider.name(),
                "destination", destination,
                "kind", deadLetter ? "dead-letter" : "queue",
                "outcome", outcome(success));
    }

    public void recordPollFailure(MessageQueueProvider provider, String destination, boolean deadLetter) {
        increment(
                "platform.message.queue.poll.failure",
                "provider", provider.name(),
                "destination", destination,
                "kind", deadLetter ? "dead-letter" : "queue");
    }

    private void increment(String name, String... tags) {
        if (meterRegistry != null) {
            meterRegistry.counter(name, tags).increment();
        }
    }

    private String outcome(boolean success) {
        return success ? "success" : "failure";
    }
}
