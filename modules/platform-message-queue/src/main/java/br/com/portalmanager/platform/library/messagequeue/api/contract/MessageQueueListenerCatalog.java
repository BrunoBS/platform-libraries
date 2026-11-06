package br.com.portalmanager.platform.library.messagequeue.api.contract;

import java.util.Collection;

/** Read-only listener metadata exposed for trusted platform integrations. */
public interface MessageQueueListenerCatalog {
    Collection<Registration> registrations();
    record Registration(String destination, String entryPoint) { }
}
