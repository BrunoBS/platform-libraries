package br.com.portalmanager.platform.library.testing.fixture;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZoneOffset;

public final class TestClock {

    private TestClock() {
    }

    public static Clock fixed(String instant) {
        return fixed(Instant.parse(instant));
    }

    public static Clock fixed(Instant instant) {
        return fixed(instant, ZoneOffset.UTC);
    }

    public static Clock fixed(String instant, ZoneId zone) {
        return fixed(Instant.parse(instant), zone);
    }

    public static Clock fixed(Instant instant, ZoneId zone) {
        if (instant == null) {
            throw new IllegalArgumentException("Test clock instant must not be null");
        }
        if (zone == null) {
            throw new IllegalArgumentException("Test clock zone must not be null");
        }
        return Clock.fixed(instant, zone);
    }
}
