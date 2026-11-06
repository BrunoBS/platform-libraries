package br.com.portalmanager.platform.library.messagequeue.internal.resolver;

import br.com.portalmanager.platform.library.messagequeue.api.configuration.MessageQueueProvider;

import java.time.Duration;

public record ResolvedDestination(
        String logicalName,
        MessageQueueProvider provider,
        String queue,
        String deadLetterReference,
        boolean publisherEnabled,
        boolean consumerEnabled,
        Duration visibilityTimeout,
        Duration waitTime,
        Integer concurrency
) {
}
