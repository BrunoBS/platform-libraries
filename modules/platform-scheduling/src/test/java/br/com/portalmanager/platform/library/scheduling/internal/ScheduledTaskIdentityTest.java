package br.com.portalmanager.platform.library.scheduling.internal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import br.com.portalmanager.platform.library.messaging.exception.PlatformConfigurationException;
import br.com.portalmanager.platform.library.scheduling.annotation.DisableScheduledLock;
import br.com.portalmanager.platform.library.scheduling.task.PlatformScheduledTask;
import java.lang.reflect.Method;
import org.junit.jupiter.api.Test;
import reactor.core.publisher.Mono;
import org.springframework.aop.framework.ProxyFactory;
import org.springframework.scheduling.config.Task;
import org.springframework.scheduling.support.ScheduledMethodRunnable;

class ScheduledTaskIdentityTest {

    @Test
    void identityIsStableAndContainsServiceMethodSignatureAndSchedule() throws NoSuchMethodException {
        Method method = SampleScheduledTasks.class.getDeclaredMethod("refresh", String.class);
        ScheduledMethodRunnable task = new ScheduledMethodRunnable(new SampleScheduledTasks(), method);

        String identity = ScheduledTaskIdentity.from(task, "fixed-rate:PT1M");

        assertThat(identity)
                .isEqualTo(SampleScheduledTasks.class.getName()
                        + "#refresh(java.lang.String):fixed-rate:PT1M");
    }

    @Test
    void unsupportedRunnableFailsInsteadOfRunningWithoutDistributedLock() {
        assertThatThrownBy(() -> ScheduledTaskIdentity.from(
                        () -> {}, "programmatic"))
                .isInstanceOf(PlatformConfigurationException.class)
                .satisfies(failure -> assertThat(((PlatformConfigurationException) failure)
                                .getErrorResponse().code())
                        .isEqualTo("PLT-SCHED-004"));
    }

    @Test
    void reactiveScheduledMethodHasAStableLockIdentity() throws NoSuchMethodException {
        Method method = SampleScheduledTasks.class.getDeclaredMethod("refreshReactive");
        ScheduledMethodRunnable task = new ScheduledMethodRunnable(new SampleScheduledTasks(), method);

        assertThat(ScheduledTaskIdentity.from(task, "scheduled"))
                .isEqualTo(SampleScheduledTasks.class.getName() + "#refreshReactive():scheduled");
    }

    @Test
    void identifiesSpringWrappedMethodThatOptsOutOfTheDistributedLock() throws NoSuchMethodException {
        Method method = SampleScheduledTasks.class.getDeclaredMethod("runWithoutLock");
        Runnable task = new Task(new ScheduledMethodRunnable(new SampleScheduledTasks(), method)).getRunnable();

        assertThat(ScheduledTaskIdentity.disablesLock(task)).isTrue();
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

        assertThat(ScheduledTaskIdentity.from(task, "scheduled"))
                .isEqualTo("programmatic:parameter-reconciliation:scheduled");
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

        assertThatThrownBy(() -> ScheduledTaskIdentity.from(task, "scheduled"))
                .isInstanceOf(PlatformConfigurationException.class)
                .satisfies(failure -> assertThat(((PlatformConfigurationException) failure)
                                .getErrorResponse().code())
                        .isEqualTo("PLT-SCHED-004"));
    }

    @Test
    void springOutcomeTrackingWrapperKeepsTheScheduledMethodIdentity() throws NoSuchMethodException {
        Method method = SampleScheduledTasks.class.getDeclaredMethod("refreshAll");
        ScheduledMethodRunnable scheduledTask = new ScheduledMethodRunnable(new SampleScheduledTasks(), method);
        Runnable registeredTask = new Task(scheduledTask).getRunnable();

        String identity = ScheduledTaskIdentity.from(registeredTask, "scheduled");

        assertThat(identity)
                .isEqualTo(SampleScheduledTasks.class.getName() + "#refreshAll():scheduled");
    }

    @Test
    void identityUsesTheTargetClassForJdkProxies() throws NoSuchMethodException {
        SampleScheduledTasks target = new SampleScheduledTasks();
        ProxyFactory proxyFactory = new ProxyFactory(target);
        proxyFactory.setInterfaces(SampleScheduledTaskContract.class);
        Object proxy = proxyFactory.getProxy();
        Method method = SampleScheduledTaskContract.class.getMethod("refresh", String.class);
        ScheduledMethodRunnable task = new ScheduledMethodRunnable(proxy, method);

        String identity = ScheduledTaskIdentity.from(task, "fixed-rate:PT1M");

        assertThat(identity)
                .isEqualTo(SampleScheduledTasks.class.getName()
                        + "#refresh(java.lang.String):fixed-rate:PT1M");
    }

    @Test
    void overloadedMethodsHaveDifferentLockIdentities() throws NoSuchMethodException {
        SampleScheduledTasks target = new SampleScheduledTasks();
        String noArguments = ScheduledTaskIdentity.from(
                new ScheduledMethodRunnable(target, SampleScheduledTasks.class.getDeclaredMethod("refresh")),
                "scheduled");
        String withTenant = ScheduledTaskIdentity.from(
                new ScheduledMethodRunnable(
                        target, SampleScheduledTasks.class.getDeclaredMethod("refresh", String.class)),
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

        @org.springframework.scheduling.annotation.Scheduled(fixedRate = 1000)
        @DisableScheduledLock
        void runWithoutLock() {}
    }
}
