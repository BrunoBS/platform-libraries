package br.com.portalmanager.platform.library.messagequeue.capability;

import br.com.portalmanager.platform.library.messagequeue.configuration.MessageQueueProperties;
import br.com.portalmanager.platform.library.messagequeue.resolver.ResolvedDestination;
import org.springframework.beans.factory.InitializingBean;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class QueueCapabilitiesRegistry implements InitializingBean {

    private final MessageQueueProperties properties;
    private final QueueCapabilitiesResolver resolver;
    private final Map<String, QueueCapabilities> capabilitiesByQueue = new ConcurrentHashMap<>();

    public QueueCapabilitiesRegistry(
            MessageQueueProperties properties,
            QueueCapabilitiesResolver resolver) {
        this.properties = properties;
        this.resolver = resolver;
    }

    @Override
    public void afterPropertiesSet() {
        properties.getDestinations().values().stream()
                .map(MessageQueueProperties.Destination::getQueue)
                .filter(queue -> queue != null && !queue.isBlank())
                .distinct()
                .forEach(this::refresh);
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
