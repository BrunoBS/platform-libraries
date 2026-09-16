package com.empresa.platform.audit.publisher;

import com.empresa.platform.audit.client.AuditEventClient;
import com.empresa.platform.audit.config.PlatformAuditProperties;
import com.empresa.platform.audit.fallback.AuditFallbackStore;
import com.empresa.platform.audit.model.AuditEventRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.core.task.TaskExecutor;

public final class DefaultAuditPublisher implements AuditPublisher {

    private static final Logger log = LoggerFactory.getLogger(DefaultAuditPublisher.class);

    private final AuditEventClient client;
    private final TaskExecutor taskExecutor;
    private final PlatformAuditProperties properties;
    private final ObjectProvider<AuditFallbackStore> fallbackStoreProvider;

    public DefaultAuditPublisher(
            AuditEventClient client,
            TaskExecutor taskExecutor,
            PlatformAuditProperties properties,
            ObjectProvider<AuditFallbackStore> fallbackStoreProvider
    ) {
        this.client = client;
        this.taskExecutor = taskExecutor;
        this.properties = properties;
        this.fallbackStoreProvider = fallbackStoreProvider;
    }

    @Override
    public void publish(AuditEventRequest event) {
        if (properties.isFailOnError()) {
            client.publish(event);
            return;
        }

        try {
            taskExecutor.execute(() -> publishAsync(event));
        } catch (Exception exception) {
            log.error(
                    "Failed to schedule audit event | resource={} | resourceId={} | action={}",
                    event.resource(),
                    event.resourceId(),
                    event.action(),
                    exception
            );
            storeFallback(event);
        }
    }

    private void publishAsync(AuditEventRequest event) {
        try {
            client.publish(event);
        } catch (Exception exception) {
            log.error(
                    "Failed to publish audit event | resource={} | resourceId={} | action={}",
                    event.resource(),
                    event.resourceId(),
                    event.action(),
                    exception
            );
            storeFallback(event);
        }
    }

    private void storeFallback(AuditEventRequest event) {
        AuditFallbackStore fallbackStore = fallbackStoreProvider.getIfAvailable();
        if (fallbackStore == null) {
            return;
        }

        try {
            fallbackStore.save(event);
        } catch (Exception exception) {
            log.error(
                    "Failed to persist audit event in fallback store | resource={} | resourceId={} | action={}",
                    event.resource(),
                    event.resourceId(),
                    event.action(),
                    exception
            );
        }
    }
}
