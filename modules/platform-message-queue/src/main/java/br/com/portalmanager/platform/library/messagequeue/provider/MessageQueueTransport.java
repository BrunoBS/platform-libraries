package br.com.portalmanager.platform.library.messagequeue.provider;

import br.com.portalmanager.platform.library.messagequeue.resolver.ResolvedDestination;

public interface MessageQueueTransport {
    void send(ResolvedDestination destination, String body);
}
