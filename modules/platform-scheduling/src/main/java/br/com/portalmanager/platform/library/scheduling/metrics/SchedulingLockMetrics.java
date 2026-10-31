package br.com.portalmanager.platform.library.scheduling.metrics;

import io.micrometer.core.instrument.MeterRegistry;

public final class SchedulingLockMetrics {
    private final MeterRegistry registry;

    public SchedulingLockMetrics(MeterRegistry registry) {
        this.registry = registry;
    }

    public void acquired() {
        if (registry != null) {
            registry.counter("golden.scheduling.lock", "outcome", "acquired").increment();
        }
    }

    public void skipped() {
        if (registry != null) {
            registry.counter("golden.scheduling.lock", "outcome", "skipped").increment();
        }
    }

    public void failed() {
        if (registry != null) {
            registry.counter("golden.scheduling.lock", "outcome", "failed").increment();
        }
    }
}
