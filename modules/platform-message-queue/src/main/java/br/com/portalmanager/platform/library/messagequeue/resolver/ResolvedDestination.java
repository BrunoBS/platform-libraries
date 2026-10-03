package br.com.portalmanager.platform.library.messagequeue.resolver;

import br.com.portalmanager.platform.library.messagequeue.configuration.MessageQueueProvider;

import java.time.Duration;

public record ResolvedDestination(
        String logicalName,
        MessageQueueProvider provider,
        String queue,
        String deadLetterQueue,
        boolean publisherEnabled,
        boolean consumerEnabled,
        Duration visibilityTimeout,
        Duration waitTime,
        Integer concurrency
) {
}
