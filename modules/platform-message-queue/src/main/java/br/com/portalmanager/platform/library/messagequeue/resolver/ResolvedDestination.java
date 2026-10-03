package br.com.portalmanager.platform.library.messagequeue.resolver;

import br.com.portalmanager.platform.library.messagequeue.configuration.MessageQueueProvider;

import java.time.Duration;

public record ResolvedDestination(
        String logicalName,
        MessageQueueProvider provider,
        String queue,
        String deadLetterReference,
        boolean publisherEnabled,
        boolean consumerEnabled,
        boolean ordered,
        Duration visibilityTimeout,
        Duration waitTime,
        Integer concurrency
) {

    public ResolvedDestination(
            String logicalName,
            MessageQueueProvider provider,
            String queue,
            String deadLetterReference,
            boolean publisherEnabled,
            boolean consumerEnabled,
            Duration visibilityTimeout,
            Duration waitTime,
            Integer concurrency) {
        this(logicalName, provider, queue, deadLetterReference, publisherEnabled, consumerEnabled,
                false, visibilityTimeout, waitTime, concurrency);
    }

}
