package com.empresa.platform.audit.recovery;

import org.springframework.beans.factory.DisposableBean;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler;

import java.time.Duration;
import java.util.concurrent.ScheduledFuture;

public final class AuditRecoveryScheduler implements InitializingBean, DisposableBean {

    private final AuditRecoveryWorker worker;
    private final ThreadPoolTaskScheduler scheduler;
    private final Duration interval;
    private ScheduledFuture<?> future;

    public AuditRecoveryScheduler(
            AuditRecoveryWorker worker,
            ThreadPoolTaskScheduler scheduler,
            Duration interval
    ) {
        this.worker = worker;
        this.scheduler = scheduler;
        this.interval = interval;
    }

    @Override
    public void afterPropertiesSet() {
        future = scheduler.scheduleWithFixedDelay(worker::recover, interval);
    }

    @Override
    public void destroy() {
        if (future != null) {
            future.cancel(false);
        }
    }
}
