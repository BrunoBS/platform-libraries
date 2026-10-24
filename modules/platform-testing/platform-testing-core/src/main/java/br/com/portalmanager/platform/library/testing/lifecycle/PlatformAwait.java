package br.com.portalmanager.platform.library.testing.lifecycle;

import java.time.Duration;
import java.util.function.BooleanSupplier;

/** Bounded polling for assertions on asynchronous test effects. */
public final class PlatformAwait {

    private PlatformAwait() {}

    public static void until(BooleanSupplier condition, Duration timeout, Duration interval) {
        if (condition == null || timeout == null || interval == null
                || timeout.isNegative() || interval.isNegative() || interval.isZero()) {
            throw new IllegalArgumentException("Condition, timeout and positive interval are required");
        }
        long deadline = System.nanoTime() + timeout.toNanos();
        while (true) {
            if (condition.getAsBoolean()) {
                return;
            }
            long remaining = deadline - System.nanoTime();
            if (remaining <= 0) {
                throw new AssertionError("Condition was not met within " + timeout);
            }
            try {
                Thread.sleep(Math.min(Math.max(1, interval.toMillis()),
                        Math.max(1, Duration.ofNanos(remaining).toMillis())));
            } catch (InterruptedException exception) {
                Thread.currentThread().interrupt();
                throw new AssertionError("Interrupted while waiting for condition", exception);
            }
        }
    }
}
