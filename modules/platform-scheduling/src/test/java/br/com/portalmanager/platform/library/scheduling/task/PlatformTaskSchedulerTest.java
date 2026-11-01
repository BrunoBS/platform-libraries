package br.com.portalmanager.platform.library.scheduling.task;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import br.com.portalmanager.platform.library.messaging.exception.PlatformConfigurationException;
import java.time.Duration;
import org.junit.jupiter.api.Test;

class PlatformTaskSchedulerTest {
    @Test
    void reportsUninitializedSchedulerWithPlatformError() {
        PlatformTaskScheduler scheduler = new PlatformTaskScheduler(() -> null);

        assertThatThrownBy(() -> scheduler.scheduleWithFixedDelay(
                        () -> {}, Duration.ofSeconds(1), PlatformScheduleOptions.withoutLock()))
                .isInstanceOf(PlatformConfigurationException.class)
                .satisfies(failure -> assertThat(
                                ((PlatformConfigurationException) failure).getErrorResponse().code())
                        .isEqualTo("PLT-SCHED-006"));
    }
}
