package br.com.portalmanager.platform.library.scheduling.task;

import java.time.Duration;
import java.time.Instant;
import java.util.Objects;
import java.util.concurrent.ScheduledFuture;
import java.util.function.Supplier;
import org.springframework.scheduling.TaskScheduler;
import org.springframework.scheduling.Trigger;

/** Schedules programmatic tasks through the distributed-locking scheduler. */
public final class PlatformTaskScheduler {
    private final Supplier<TaskScheduler> schedulerSupplier;

    public PlatformTaskScheduler(Supplier<TaskScheduler> schedulerSupplier) {
        this.schedulerSupplier = Objects.requireNonNull(schedulerSupplier);
    }

    public ScheduledFuture<?> schedule(String lockName, Runnable task, Trigger trigger) {
        return schedule(task, trigger, PlatformScheduleOptions.withLock(lockName));
    }

    public ScheduledFuture<?> schedule(String lockName, Runnable task, Instant startTime) {
        return schedule(task, startTime, PlatformScheduleOptions.withLock(lockName));
    }

    public ScheduledFuture<?> scheduleAtFixedRate(String lockName, Runnable task, Duration period) {
        return scheduleAtFixedRate(task, period, PlatformScheduleOptions.withLock(lockName));
    }

    public ScheduledFuture<?> scheduleWithFixedDelay(String lockName, Runnable task, Duration delay) {
        return scheduleWithFixedDelay(task, delay, PlatformScheduleOptions.withLock(lockName));
    }

    public ScheduledFuture<?> schedule(Runnable task, Trigger trigger, PlatformScheduleOptions options) {
        return scheduler().schedule(identified(task, options), trigger);
    }

    public ScheduledFuture<?> schedule(Runnable task, Instant startTime, PlatformScheduleOptions options) {
        return scheduler().schedule(identified(task, options), startTime);
    }

    public ScheduledFuture<?> scheduleAtFixedRate(
            Runnable task, Duration period, PlatformScheduleOptions options) {
        return scheduler().scheduleAtFixedRate(identified(task, options), period);
    }

    public ScheduledFuture<?> scheduleWithFixedDelay(
            Runnable task, Duration delay, PlatformScheduleOptions options) {
        return scheduler().scheduleWithFixedDelay(identified(task, options), delay);
    }

    private TaskScheduler scheduler() {
        TaskScheduler scheduler = schedulerSupplier.get();
        if (scheduler == null) {
            throw new IllegalStateException("Platform scheduling has not been initialized yet");
        }
        return scheduler;
    }

    private static PlatformScheduledTask identified(Runnable task, PlatformScheduleOptions options) {
        Objects.requireNonNull(task, "task");
        Objects.requireNonNull(options, "options");
        return programmaticTask(options.lockName(), task, options.lockEnabled());
    }

    private static PlatformScheduledTask programmaticTask(String lockName, Runnable task, boolean lockEnabled) {
        return new PlatformScheduledTask() {
            @Override
            public String lockName() {
                return lockName;
            }

            @Override
            public boolean lockEnabled() {
                return lockEnabled;
            }

            @Override
            public void run() {
                task.run();
            }
        };
    }
}
