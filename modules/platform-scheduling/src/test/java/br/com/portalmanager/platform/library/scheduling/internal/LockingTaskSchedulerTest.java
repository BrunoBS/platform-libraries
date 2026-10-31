package br.com.portalmanager.platform.library.scheduling.internal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import br.com.portalmanager.platform.library.scheduling.configuration.PlatformSchedulingProperties;
import br.com.portalmanager.platform.library.scheduling.metrics.SchedulingLockMetrics;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import java.lang.reflect.Method;
import java.time.Clock;
import java.time.Instant;
import java.util.Optional;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.atomic.AtomicInteger;
import net.javacrumbs.shedlock.core.LockProvider;
import net.javacrumbs.shedlock.core.SimpleLock;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.scheduling.TaskScheduler;
import org.springframework.scheduling.support.ScheduledMethodRunnable;

class LockingTaskSchedulerTest {
    private static final String SERVICE_NAME = "scheduling-unit-test";

    @Test
    void skipsTaskWhenAnotherInstanceOwnsTheLock() throws NoSuchMethodException {
        TaskScheduler delegate = mock(TaskScheduler.class);
        ScheduledFuture<?> scheduledFuture = mock(ScheduledFuture.class);
        doReturn(scheduledFuture).when(delegate).schedule(any(Runnable.class), any(Instant.class));
        LockProvider lockProvider = configuration -> Optional.empty();
        SimpleMeterRegistry registry = new SimpleMeterRegistry();
        AtomicInteger executions = new AtomicInteger();
        LockingTaskScheduler scheduler = scheduler(delegate, lockProvider, registry);

        scheduler.schedule(task(executions), Instant.now());
        ArgumentCaptor<Runnable> registered = ArgumentCaptor.forClass(Runnable.class);
        verify(delegate).schedule(registered.capture(), any(Instant.class));

        registered.getValue().run();

        assertThat(executions).hasValue(0);
        assertThat(registry.counter("golden.scheduling.lock", "outcome", "skipped").count()).isEqualTo(1);
        assertThat(registry.counter("golden.scheduling.lock", "outcome", "failed").count()).isZero();
    }

    @Test
    void failsClosedWhenRedisCannotAcquireTheLock() throws NoSuchMethodException {
        TaskScheduler delegate = mock(TaskScheduler.class);
        doReturn(mock(ScheduledFuture.class)).when(delegate).schedule(any(Runnable.class), any(Instant.class));
        LockProvider lockProvider = configuration -> {
            throw new IllegalStateException("Redis unavailable");
        };
        SimpleMeterRegistry registry = new SimpleMeterRegistry();
        AtomicInteger executions = new AtomicInteger();
        LockingTaskScheduler scheduler = scheduler(delegate, lockProvider, registry);

        scheduler.schedule(task(executions), Instant.now());
        ArgumentCaptor<Runnable> registered = ArgumentCaptor.forClass(Runnable.class);
        verify(delegate).schedule(registered.capture(), any(Instant.class));

        registered.getValue().run();

        assertThat(executions).hasValue(0);
        assertThat(registry.counter("golden.scheduling.lock", "outcome", "failed").count()).isEqualTo(1);
        assertThat(registry.counter("golden.scheduling.lock", "outcome", "acquired").count()).isZero();
    }

    @Test
    void runsTaskWhenLockIsAcquired() throws NoSuchMethodException {
        TaskScheduler delegate = mock(TaskScheduler.class);
        doReturn(mock(ScheduledFuture.class)).when(delegate).schedule(any(Runnable.class), any(Instant.class));
        SimpleLock lock = mock(SimpleLock.class);
        LockProvider lockProvider = configuration -> Optional.of(lock);
        SimpleMeterRegistry registry = new SimpleMeterRegistry();
        AtomicInteger executions = new AtomicInteger();
        LockingTaskScheduler scheduler = scheduler(delegate, lockProvider, registry);

        scheduler.schedule(task(executions), Instant.now());
        ArgumentCaptor<Runnable> registered = ArgumentCaptor.forClass(Runnable.class);
        verify(delegate).schedule(registered.capture(), any(Instant.class));

        registered.getValue().run();

        assertThat(executions).hasValue(1);
        assertThat(registry.counter("golden.scheduling.lock", "outcome", "acquired").count()).isEqualTo(1);
        verify(lock).unlock();
    }

    private static LockingTaskScheduler scheduler(
            TaskScheduler delegate, LockProvider lockProvider, SimpleMeterRegistry registry) {
        when(delegate.getClock()).thenReturn(Clock.systemUTC());
        PlatformSchedulingProperties properties = new PlatformSchedulingProperties();
        properties.setKeepAlive(false);
        return new LockingTaskScheduler(
                delegate, lockProvider, SERVICE_NAME, properties, new SchedulingLockMetrics(registry));
    }

    private static ScheduledMethodRunnable task(AtomicInteger executions) throws NoSuchMethodException {
        Method method = TaskProbe.class.getDeclaredMethod("runTask");
        return new ScheduledMethodRunnable(new TaskProbe(executions), method);
    }

    static class TaskProbe {
        private final AtomicInteger executions;

        TaskProbe(AtomicInteger executions) {
            this.executions = executions;
        }

        void runTask() {
            executions.incrementAndGet();
        }
    }
}
