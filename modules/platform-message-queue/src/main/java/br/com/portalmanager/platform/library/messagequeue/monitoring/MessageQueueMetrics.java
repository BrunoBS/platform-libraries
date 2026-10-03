package br.com.portalmanager.platform.library.messagequeue.monitoring;

import br.com.portalmanager.platform.library.messagequeue.configuration.MessageQueueProvider;
import br.com.portalmanager.platform.library.messagequeue.resolver.ResolvedDestination;
import io.micrometer.core.instrument.MeterRegistry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class MessageQueueMetrics {

    private static final Logger LOGGER = LoggerFactory.getLogger(MessageQueueMetrics.class);

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

    public void recordAcknowledgementFailure(
            MessageQueueProvider provider,
            String destination,
            boolean deadLetter) {
        increment(
                "platform.message.queue.ack.failure",
                "provider", provider.name(),
                "destination", destination,
                "kind", deadLetter ? "dead-letter" : "queue");
    }

    public void recordPollFailure(MessageQueueProvider provider, String destination, boolean deadLetter) {
        increment(
                "platform.message.queue.poll.failure",
                "provider", provider.name(),
                "destination", destination,
                "kind", deadLetter ? "dead-letter" : "queue");
    }

    private void increment(String name, String... tags) {
        if (meterRegistry == null) {
            return;
        }
        try {
            meterRegistry.counter(name, tags).increment();
        } catch (RuntimeException exception) {
            LOGGER.warn("Could not record message queue metric '{}'", name, exception);
        }
    }

    private String outcome(boolean success) {
        return success ? "success" : "failure";
    }
}
