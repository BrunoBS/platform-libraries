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
        return scheduler().schedule(identified(lockName, task), trigger);
    }

    public ScheduledFuture<?> schedule(String lockName, Runnable task, Instant startTime) {
        return scheduler().schedule(identified(lockName, task), startTime);
    }

    public ScheduledFuture<?> scheduleAtFixedRate(String lockName, Runnable task, Duration period) {
        return scheduler().scheduleAtFixedRate(identified(lockName, task), period);
    }

    public ScheduledFuture<?> scheduleWithFixedDelay(String lockName, Runnable task, Duration delay) {
        return scheduler().scheduleWithFixedDelay(identified(lockName, task), delay);
    }

    private TaskScheduler scheduler() {
        TaskScheduler scheduler = schedulerSupplier.get();
        if (scheduler == null) {
            throw new IllegalStateException("Platform scheduling has not been initialized yet");
        }
        return scheduler;
    }

    private static PlatformScheduledTask identified(String lockName, Runnable task) {
        Objects.requireNonNull(task, "task");
        return new PlatformScheduledTask() {
            @Override
            public String lockName() {
                return lockName;
            }

            @Override
            public void run() {
                task.run();
            }
        };
    }
}
