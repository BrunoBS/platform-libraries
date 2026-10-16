package br.com.portalmanager.platform.library.redis.core;

import java.util.concurrent.atomic.LongAdder;

/** Process-local monotonic counters; no payload/key labels. */
public final class RedisListenerMetrics {
    private final LongAdder processed = new LongAdder(),
            failed = new LongAdder(),
            rejected = new LongAdder();

    public void processed() {
        processed.increment();
    }

    public void failed() {
        failed.increment();
    }

    public void rejected() {
        rejected.increment();
    }

    public long processedCount() {
        return processed.sum();
    }

    public long failureCount() {
        return failed.sum();
    }

    public long rejectedCount() {
        return rejected.sum();
    }
}
