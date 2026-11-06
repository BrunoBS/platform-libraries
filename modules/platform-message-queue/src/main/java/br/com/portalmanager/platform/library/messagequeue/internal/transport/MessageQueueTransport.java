package br.com.portalmanager.platform.library.messagequeue.internal.transport;

import br.com.portalmanager.platform.library.messagequeue.api.contract.MessageQueuePublishOptions;
import br.com.portalmanager.platform.library.messagequeue.internal.resolver.ResolvedDestination;

public interface MessageQueueTransport {

    void send(ResolvedDestination destination, String body);

    default void send(ResolvedDestination destination, String body, MessageQueuePublishOptions options) {
        if (options != null && (options.orderingKey() != null || options.deduplicationId() != null)) {
            throw new UnsupportedOperationException("This transport does not support ordered publish options");
        }
        send(destination, body);
    }
}
