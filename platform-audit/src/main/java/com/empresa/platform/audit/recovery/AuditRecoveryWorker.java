package com.empresa.platform.audit.recovery;

import com.empresa.platform.audit.client.AuditEventClient;
import com.empresa.platform.audit.config.PlatformAuditProperties;
import com.empresa.platform.audit.fallback.AuditFallbackStore;
import com.empresa.platform.audit.model.AuditEventRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class AuditRecoveryWorker {

    private static final Logger log = LoggerFactory.getLogger(AuditRecoveryWorker.class);

    private final AuditEventClient client;
    private final AuditFallbackStore fallbackStore;
    private final PlatformAuditProperties properties;

    public AuditRecoveryWorker(
            AuditEventClient client,
            AuditFallbackStore fallbackStore,
            PlatformAuditProperties properties
    ) {
        this.client = client;
        this.fallbackStore = fallbackStore;
        this.properties = properties;
    }

    public void recover() {
        try {
            recoverBatch();
        } catch (Exception exception) {
            log.warn("Audit recovery cycle failed before completion", exception);
        }
    }

    private void recoverBatch() {
        if (!fallbackStore.hasPending()) {
            return;
        }

        int recovered = 0;
        int batchSize = properties.getFallback().getBatchSize();

        while (recovered < batchSize) {
            AuditEventRequest event = fallbackStore.peek();
            if (event == null) {
                return;
            }

            try {
                client.publish(event);
                fallbackStore.removeHead();
                recovered++;
            } catch (Exception exception) {
                log.warn(
                        "Audit recovery paused after publish failure | recovered={} | resource={} | resourceId={} | action={}",
                        recovered,
                        event.resource(),
                        event.resourceId(),
                        event.action(),
                        exception
                );
                return;
            }
        }

        if (recovered > 0) {
            log.info("Audit recovery cycle completed | recovered={}", recovered);
        }
    }
}
