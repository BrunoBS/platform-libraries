package br.com.portalmanager.platform.library.messagequeue.api.contract;

/** Read-only resolved destination metadata exposed for trusted platform integrations. */
public interface MessageQueueDestinationCatalog {
    ResolvedDestination resolveForIntegration(String destination);
    record ResolvedDestination(String destination, String resourceIdentifier, boolean consumerEnabled) { }
}
