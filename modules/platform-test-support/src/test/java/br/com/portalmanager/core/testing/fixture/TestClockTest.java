package br.com.portalmanager.core.testing.fixture;

import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZoneOffset;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class TestClockTest {

    @Test
    void shouldCreateFixedUtcClock() {
        Clock clock = TestClock.fixed("2026-09-12T12:00:00Z");

        assertThat(clock.instant()).isEqualTo(Instant.parse("2026-09-12T12:00:00Z"));
        assertThat(clock.getZone()).isEqualTo(ZoneOffset.UTC);
    }

    @Test
    void shouldCreateFixedClockWithCustomZone() {
        ZoneId zone = ZoneId.of("America/Sao_Paulo");
        Clock clock = TestClock.fixed("2026-09-12T12:00:00Z", zone);

        assertThat(clock.instant()).isEqualTo(Instant.parse("2026-09-12T12:00:00Z"));
        assertThat(clock.getZone()).isEqualTo(zone);
    }

    @Test
    void shouldRejectNullInstant() {
        assertThatThrownBy(() -> TestClock.fixed((Instant) null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("instant");
    }
}
