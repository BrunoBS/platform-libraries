package br.com.portalmanager.platform.library.testing.lifecycle;

import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class PlatformAwaitTest {

    @Test
    void observesAConditionThatEventuallyBecomesTrue() {
        AtomicInteger checks = new AtomicInteger();
        PlatformAwait.until(() -> checks.incrementAndGet() == 2,
                Duration.ofSeconds(1), Duration.ofMillis(1));
        assertThat(checks.get()).isEqualTo(2);
    }

    @Test
    void reportsTimeoutWithoutSilentlyPassing() {
        assertThatThrownBy(() -> PlatformAwait.until(() -> false,
                Duration.ZERO, Duration.ofMillis(1)))
                .isInstanceOf(AssertionError.class)
                .hasMessageContaining("Condition was not met");
    }
}
