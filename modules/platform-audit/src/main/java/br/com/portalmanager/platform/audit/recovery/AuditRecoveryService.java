package br.com.portalmanager.platform.audit.recovery;

import br.com.portalmanager.platform.audit.config.PlatformAuditProperties;
import br.com.portalmanager.platform.audit.fallback.AuditFallbackStore;
import br.com.portalmanager.platform.audit.model.AuditEventRequest;
import br.com.portalmanager.platform.audit.publisher.AuditPublisher;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler;

import java.util.Optional;
import java.util.concurrent.ScheduledFuture;

public final class AuditRecoveryService implements InitializingBean, DisposableBean {

    private static final Logger log = LoggerFactory.getLogger(AuditRecoveryService.class);

    private final AuditPublisher publisher;
    private final AuditFallbackStore fallbackStore;
    private final AuditRecoveryLock recoveryLock;
    private final ThreadPoolTaskScheduler scheduler;
    private final PlatformAuditProperties properties;
    private ScheduledFuture<?> future;

    public AuditRecoveryService(
            AuditPublisher publisher,
            AuditFallbackStore fallbackStore,
            AuditRecoveryLock recoveryLock,
            ThreadPoolTaskScheduler scheduler,
            PlatformAuditProperties properties
    ) {
        this.publisher = publisher;
        this.fallbackStore = fallbackStore;
        this.recoveryLock = recoveryLock;
        this.scheduler = scheduler;
        this.properties = properties;
    }

    @Override
    public void afterPropertiesSet() {
        future = scheduler.scheduleWithFixedDelay(
                this::recover,
                properties.getFallback().getRecoveryInterval()
        );
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
                publisher.publishDirect(event);
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

    private void releaseLock(String token) {
        try {
            recoveryLock.release(token);
        } catch (Exception exception) {
            log.warn("Failed to release audit recovery lock", exception);
        }
    }

    @Override
    public void destroy() {
        if (future != null) {
            future.cancel(false);
        }
    }
}
