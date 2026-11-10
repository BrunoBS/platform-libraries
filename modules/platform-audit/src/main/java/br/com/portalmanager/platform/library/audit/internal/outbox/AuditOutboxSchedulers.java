package br.com.portalmanager.platform.library.audit.internal.outbox;

import br.com.portalmanager.platform.library.scheduling.api.contract.task.PlatformTaskScheduler;
import java.time.Duration;
import java.util.concurrent.ScheduledFuture;

/** Two distributed, bounded recovery cycles: short retries and daily retries. */
public final class AuditOutboxSchedulers implements AutoCloseable {
    private final PlatformTaskScheduler scheduler;
    private final AuditOutboxProcessor processor;
    private ScheduledFuture<?> shortCycle;
    private ScheduledFuture<?> dailyCycle;

    public AuditOutboxSchedulers(PlatformTaskScheduler scheduler, AuditOutboxProcessor processor) {
        this.scheduler = scheduler;
        this.processor = processor;
    }

    public synchronized void start() {
        shortCycle = scheduler.scheduleWithFixedDelay("platform-audit-outbox-short-retry",
                processor::processFastBatch, Duration.ofMinutes(10));
        dailyCycle = scheduler.scheduleWithFixedDelay("platform-audit-outbox-daily-retry",
                processor::processDailyBatch, Duration.ofDays(1));
    }

    @Override
    public synchronized void close() {
        if (shortCycle != null) shortCycle.cancel(false);
        if (dailyCycle != null) dailyCycle.cancel(false);
    }
}
