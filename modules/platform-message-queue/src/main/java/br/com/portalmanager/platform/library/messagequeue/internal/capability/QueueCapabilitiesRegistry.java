package br.com.portalmanager.platform.library.messagequeue.internal.capability;

import br.com.portalmanager.platform.library.messagequeue.internal.resolver.ResolvedDestination;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class QueueCapabilitiesRegistry {

    private final QueueCapabilitiesResolver resolver;
    private final Map<String, QueueCapabilities> capabilitiesByQueue = new ConcurrentHashMap<>();

    public QueueCapabilitiesRegistry(QueueCapabilitiesResolver resolver) {
        this.resolver = resolver;
    }

    public QueueCapabilities get(ResolvedDestination destination) {
        return get(destination.queue());
    }

    public QueueCapabilities get(String queue) {
        return capabilitiesByQueue.computeIfAbsent(queue, resolver::resolve);
    }

    public QueueCapabilities refresh(String queue) {
        QueueCapabilities capabilities = resolver.resolve(queue);
        capabilitiesByQueue.put(queue, capabilities);
        return capabilities;
    }

    public void invalidate(String queue) {
        capabilitiesByQueue.remove(queue);
    }
}
