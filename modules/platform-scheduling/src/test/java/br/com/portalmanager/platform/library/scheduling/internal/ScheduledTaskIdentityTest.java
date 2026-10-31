package br.com.portalmanager.platform.library.scheduling.internal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalStateException;

import java.lang.reflect.Method;
import org.junit.jupiter.api.Test;
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

    static class SampleScheduledTasks {
        void refresh(String tenant) {}
    }
}
