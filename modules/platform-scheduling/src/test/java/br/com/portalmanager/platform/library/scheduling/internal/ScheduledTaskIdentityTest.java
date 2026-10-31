package br.com.portalmanager.platform.library.scheduling.internal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalStateException;

import br.com.portalmanager.platform.library.scheduling.task.PlatformScheduledTask;
import java.lang.reflect.Method;
import org.junit.jupiter.api.Test;
import reactor.core.publisher.Mono;
import org.springframework.aop.framework.ProxyFactory;
import org.springframework.scheduling.config.Task;
import org.springframework.scheduling.support.ScheduledMethodRunnable;

class ScheduledTaskIdentityTest {
    private static final String SERVICE_NAME = "audit-service";

    @Test
    void identityIsStableAndContainsServiceMethodSignatureAndSchedule() throws NoSuchMethodException {
        Method method = SampleScheduledTasks.class.getDeclaredMethod("refresh", String.class);
        ScheduledMethodRunnable task = new ScheduledMethodRunnable(new SampleScheduledTasks(), method);

        String identity = ScheduledTaskIdentity.from(task, SERVICE_NAME, "fixed-rate:PT1M");

        assertThat(identity)
                .isEqualTo(SERVICE_NAME + ":" + SampleScheduledTasks.class.getName()
                        + "#refresh(java.lang.String):fixed-rate:PT1M");
    }

    @Test
    void unsupportedRunnableFailsInsteadOfRunningWithoutDistributedLock() {
        assertThatIllegalStateException()
                .isThrownBy(() -> ScheduledTaskIdentity.from(
                        () -> {}, SERVICE_NAME, "programmatic"))
                .withMessageContaining("stable distributed-lock identity");
    }

    @Test
    void reactiveScheduledMethodIsRejectedBeforeItCanRunWithoutTheLock() throws NoSuchMethodException {
        Method method = SampleScheduledTasks.class.getDeclaredMethod("refreshReactive");
        ScheduledMethodRunnable task = new ScheduledMethodRunnable(new SampleScheduledTasks(), method);

        assertThatIllegalStateException()
                .isThrownBy(() -> ScheduledTaskIdentity.from(task, SERVICE_NAME, "scheduled"))
                .withMessageContaining("Reactive @Scheduled methods are unsupported");
    }

    @Test
    void programmaticTaskUsesItsExplicitStableIdentity() {
        PlatformScheduledTask task = new PlatformScheduledTask() {
            @Override
            public String lockName() {
                return "parameter-reconciliation";
            }

            @Override
            public void run() {}
        };

        assertThat(ScheduledTaskIdentity.from(task, SERVICE_NAME, "scheduled"))
                .isEqualTo(SERVICE_NAME + ":programmatic:parameter-reconciliation:scheduled");
    }

    @Test
    void programmaticTaskRejectsBlankIdentity() {
        PlatformScheduledTask task = new PlatformScheduledTask() {
            @Override
            public String lockName() {
                return " ";
            }

            @Override
            public void run() {}
        };

        assertThatIllegalStateException()
                .isThrownBy(() -> ScheduledTaskIdentity.from(task, SERVICE_NAME, "scheduled"))
                .withMessageContaining("stable lock name");
    }

    @Test
    void springOutcomeTrackingWrapperKeepsTheScheduledMethodIdentity() throws NoSuchMethodException {
        Method method = SampleScheduledTasks.class.getDeclaredMethod("refreshAll");
        ScheduledMethodRunnable scheduledTask = new ScheduledMethodRunnable(new SampleScheduledTasks(), method);
        Runnable registeredTask = new Task(scheduledTask).getRunnable();

        String identity = ScheduledTaskIdentity.from(registeredTask, SERVICE_NAME, "scheduled");

        assertThat(identity)
                .isEqualTo(SERVICE_NAME + ":" + SampleScheduledTasks.class.getName() + "#refreshAll():scheduled");
    }

    @Test
    void identityUsesTheTargetClassForJdkProxies() throws NoSuchMethodException {
        SampleScheduledTasks target = new SampleScheduledTasks();
        ProxyFactory proxyFactory = new ProxyFactory(target);
        proxyFactory.setInterfaces(SampleScheduledTaskContract.class);
        Object proxy = proxyFactory.getProxy();
        Method method = SampleScheduledTaskContract.class.getMethod("refresh", String.class);
        ScheduledMethodRunnable task = new ScheduledMethodRunnable(proxy, method);

        String identity = ScheduledTaskIdentity.from(task, SERVICE_NAME, "fixed-rate:PT1M");

        assertThat(identity)
                .isEqualTo(SERVICE_NAME + ":" + SampleScheduledTasks.class.getName()
                        + "#refresh(java.lang.String):fixed-rate:PT1M");
    }

    @Test
    void overloadedMethodsHaveDifferentLockIdentities() throws NoSuchMethodException {
        SampleScheduledTasks target = new SampleScheduledTasks();
        String noArguments = ScheduledTaskIdentity.from(
                new ScheduledMethodRunnable(target, SampleScheduledTasks.class.getDeclaredMethod("refresh")),
                SERVICE_NAME,
                "scheduled");
        String withTenant = ScheduledTaskIdentity.from(
                new ScheduledMethodRunnable(
                        target, SampleScheduledTasks.class.getDeclaredMethod("refresh", String.class)),
                SERVICE_NAME,
                "scheduled");

        assertThat(noArguments).isNotEqualTo(withTenant);
    }

    interface SampleScheduledTaskContract {
        void refresh(String tenant);
    }

    static class SampleScheduledTasks implements SampleScheduledTaskContract {
        void refresh() {}

        @Override
        public void refresh(String tenant) {}

        void refreshAll() {}

        Mono<Void> refreshReactive() {
            return Mono.empty();
        }
    }
}
