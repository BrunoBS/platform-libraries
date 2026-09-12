package com.empresa.platform.testing.fixture;

import java.nio.charset.StandardCharsets;
import java.util.UUID;

public final class TestIds {

    private TestIds() {
    }

    public static UUID uuid(String seed) {
        if (seed == null || seed.isBlank()) {
            throw new IllegalArgumentException("Test ID seed must not be blank");
        }
        return UUID.nameUUIDFromBytes(seed.getBytes(StandardCharsets.UTF_8));
    }
}
