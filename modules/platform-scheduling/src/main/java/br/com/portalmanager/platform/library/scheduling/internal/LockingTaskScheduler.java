package br.com.portalmanager.platform.library.scheduling.internal;

import br.com.portalmanager.platform.library.scheduling.configuration.PlatformSchedulingProperties;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.atomic.AtomicBoolean;
import net.javacrumbs.shedlock.core.DefaultLockingTaskExecutor;
import net.javacrumbs.shedlock.core.LockConfiguration;
import net.javacrumbs.shedlock.core.LockProvider;
import net.javacrumbs.shedlock.core.LockingTaskExecutor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.SchedulingAwareRunnable;
import org.springframework.scheduling.TaskScheduler;
import org.springframework.scheduling.Trigger;

/** Decorates the scheduler selected by Spring; schedule timing remains delegated unchanged. */
public final class LockingTaskScheduler implements TaskScheduler {
    private static final Logger LOGGER = LoggerFactory.getLogger(LockingTaskScheduler.class);
    private final TaskScheduler delegate;
    private final LockingTaskExecutor lockExecutor;
    private final String serviceName;
    private final PlatformSchedulingProperties properties;
    private final AtomicBoolean redisFailureLogged = new AtomicBoolean();
    private final Object lifecycleMonitor = new Object();
    private int activeExecutions;
    private boolean shuttingDown;

    public LockingTaskScheduler(
            TaskScheduler delegate,
            LockProvider lockProvider,
            String serviceName,
            PlatformSchedulingProperties properties) {
        this.delegate = delegate;
        this.lockExecutor = new DefaultLockingTaskExecutor(lockProvider);
        this.serviceName = serviceName;
        this.properties = properties;
    }

    @Override
    public Clock getClock() {
        return delegate.getClock();
    }

    @Override
    public ScheduledFuture<?> schedule(Runnable task, Trigger trigger) {
        return delegate.schedule(guard(task), trigger);
    }

    @Override
    public ScheduledFuture<?> schedule(Runnable task, Instant startTime) {
        return delegate.schedule(guard(task), startTime);
    }

    @Override
    public ScheduledFuture<?> scheduleAtFixedRate(Runnable task, Instant startTime, Duration period) {
        return delegate.scheduleAtFixedRate(guard(task), startTime, period);
    }

    @Override
    public ScheduledFuture<?> scheduleAtFixedRate(Runnable task, Duration period) {
        return delegate.scheduleAtFixedRate(guard(task), period);
    }

    @Override
    public ScheduledFuture<?> scheduleWithFixedDelay(Runnable task, Instant startTime, Duration delay) {
        return delegate.scheduleWithFixedDelay(guard(task), startTime, delay);
    }

    @Override
    public ScheduledFuture<?> scheduleWithFixedDelay(Runnable task, Duration delay) {
        return delegate.scheduleWithFixedDelay(guard(task), delay);
    }

    private Runnable guard(Runnable task) {
        if (ScheduledTaskIdentity.disablesLock(task)) {
            return task;
        }
        String lockName = ScheduledTaskIdentity.from(task, serviceName, "scheduled");
        String qualifier = task instanceof SchedulingAwareRunnable aware ? aware.getQualifier() : null;
        return new LockedRunnable(task, qualifier, lockName);
    }

    private final class LockedRunnable implements SchedulingAwareRunnable {
        private final Runnable task;
        private final String qualifier;
        private final String lockName;

        private LockedRunnable(Runnable task, String qualifier, String lockName) {
            this.task = task;
            this.qualifier = qualifier;
            this.lockName = lockName;
        }

        @Override
        public String getQualifier() {
            return qualifier;
        }

        @Override
        public boolean isLongLived() {
            return task instanceof SchedulingAwareRunnable aware && aware.isLongLived();
        }

        @Override
        public void run() {
            if (!beginExecution()) {
                return;
            }
            try {
                executeLockedTask();
            } finally {
                endExecution();
            }
        }

        private void executeLockedTask() {
            AtomicBoolean taskStarted = new AtomicBoolean();
            LockConfiguration lock = new LockConfiguration(
                    Instant.now(getClock()), lockName,
                    properties.getLockAtMostFor(), properties.getLockAtLeastFor());
            try {
                lockExecutor.executeWithLock((Runnable) () -> {
                    taskStarted.set(true);
                    task.run();
                }, lock);
            } catch (RuntimeException failure) {
                if (taskStarted.get()) {
                    redisFailureLogged.set(false);
                    throw failure;
                }
                if (redisFailureLogged.compareAndSet(false, true)) {
                    LOGGER.atError()
                            .addKeyValue("service", serviceName)
                            .addKeyValue("lockName", lockName)
                            .addKeyValue("errorCode", "PLT-SCHED-002")
                            .log("Distributed scheduled lock unavailable; execution skipped");
                }
                return;
            }
            redisFailureLogged.set(false);
        }
    }

    private boolean beginExecution() {
        synchronized (lifecycleMonitor) {
            if (shuttingDown) {
                return false;
            }
            activeExecutions++;
            return true;
        }
    }

    private void endExecution() {
        synchronized (lifecycleMonitor) {
            activeExecutions--;
            lifecycleMonitor.notifyAll();
        }
    }

    /** Stop admitting task runs and wait for current lock releases before Redis is destroyed. */
    public void shutdownAndAwaitInFlightTasks() {
        boolean interrupted = false;
        synchronized (lifecycleMonitor) {
            shuttingDown = true;
            while (activeExecutions > 0) {
                try {
                    lifecycleMonitor.wait();
                } catch (InterruptedException ignored) {
                    interrupted = true;
                }
            }
        }
        if (interrupted) {
            Thread.currentThread().interrupt();
        }
    }
}
