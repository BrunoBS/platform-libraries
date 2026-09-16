package com.empresa.platform.audit.recovery;

import com.empresa.platform.audit.client.AuditEventClient;
import com.empresa.platform.audit.config.PlatformAuditProperties;
import com.empresa.platform.audit.fallback.AuditFallbackStore;
import com.empresa.platform.audit.model.AuditEventRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Optional;

public final class AuditRecoveryWorker {

    private static final Logger log = LoggerFactory.getLogger(AuditRecoveryWorker.class);

    private final AuditEventClient client;
    private final AuditFallbackStore fallbackStore;
    private final AuditRecoveryLock recoveryLock;
    private final PlatformAuditProperties properties;

    public AuditRecoveryWorker(
            AuditEventClient client,
            AuditFallbackStore fallbackStore,
            AuditRecoveryLock recoveryLock,
            PlatformAuditProperties properties
    ) {
        this.client = client;
        this.fallbackStore = fallbackStore;
        this.recoveryLock = recoveryLock;
        this.properties = properties;
    }

    public void recover() {
        Optional<String> token = Optional.empty();

        try {
            token = recoveryLock.tryAcquire();
            if (token.isEmpty()) {
                return;
            }

            recoverBatch();
        } catch (Exception exception) {
            log.warn("Audit recovery cycle failed before completion", exception);
        } finally {
            token.ifPresent(this::releaseLock);
        }
    }

    private void releaseLock(String token) {
        try {
            recoveryLock.release(token);
        } catch (Exception exception) {
            log.warn("Failed to release audit recovery lock", exception);
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
