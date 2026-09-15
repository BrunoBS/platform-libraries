package com.empresa.platform.testing.extension;

import org.junit.jupiter.api.extension.AfterAllCallback;
import org.junit.jupiter.api.extension.AfterTestExecutionCallback;
import org.junit.jupiter.api.extension.BeforeAllCallback;
import org.junit.jupiter.api.extension.BeforeTestExecutionCallback;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

public final class TestPerformanceExtension implements
        BeforeAllCallback,
        BeforeTestExecutionCallback,
        AfterTestExecutionCallback,
        AfterAllCallback {

    private static final Logger log = LoggerFactory.getLogger(TestPerformanceExtension.class);
    private static final ExtensionContext.Namespace NAMESPACE =
            ExtensionContext.Namespace.create(TestPerformanceExtension.class);

    private static final String CLASS_START = "class-start";
    private static final String TEST_START = "test-start";
    private static final String TEST_TIMINGS = "test-timings";

    @Override
    public void beforeAll(ExtensionContext context) {
        context.getStore(NAMESPACE).put(CLASS_START, Instant.now());
        context.getStore(NAMESPACE).put(TEST_TIMINGS, new ArrayList<TestTiming>());
    }

    @Override
    public void beforeTestExecution(ExtensionContext context) {
        context.getStore(NAMESPACE).put(TEST_START, Instant.now());
    }

    @Override
    @SuppressWarnings("unchecked")
    public void afterTestExecution(ExtensionContext context) {
        Instant startedAt = context.getStore(NAMESPACE).remove(TEST_START, Instant.class);
        if (startedAt == null) {
            return;
        }

        long elapsedMs = Duration.between(startedAt, Instant.now()).toMillis();
        List<TestTiming> timings = (List<TestTiming>) context.getStore(NAMESPACE)
                .get(TEST_TIMINGS, List.class);

        if (timings != null) {
            timings.add(new TestTiming(context.getDisplayName(), elapsedMs));
        }

        long slowThresholdMs = slowThresholdMs();
        if (elapsedMs >= slowThresholdMs) {
            log.warn(
                    "[TEST-PERF] slow-test={} duration={}ms threshold={}ms",
                    context.getDisplayName(),
                    elapsedMs,
                    slowThresholdMs
            );
        }
    }

    @Override
    @SuppressWarnings("unchecked")
    public void afterAll(ExtensionContext context) {
        Instant startedAt = context.getStore(NAMESPACE).remove(CLASS_START, Instant.class);
        List<TestTiming> timings = (List<TestTiming>) context.getStore(NAMESPACE)
                .remove(TEST_TIMINGS, List.class);

        if (startedAt == null) {
            return;
        }

        long totalMs = Duration.between(startedAt, Instant.now()).toMillis();
        String className = context.getRequiredTestClass().getSimpleName();

        log.info(
                "[TEST-PERF] class={} total={}ms tests={}",
                className,
                totalMs,
                timings == null ? 0 : timings.size()
        );

        if (timings == null || timings.isEmpty()) {
            return;
        }

        timings.stream()
                .sorted(Comparator.comparingLong(TestTiming::durationMs).reversed())
                .limit(5)
                .forEach(timing -> log.info(
                        "[TEST-PERF] class={} slowest-test={} duration={}ms",
                        className,
                        timing.name(),
                        timing.durationMs()
                ));
    }

    private long slowThresholdMs() {
        return Long.getLong(
                "platform.testing.performance.slow-test-ms",
                2_000L
        );
    }

    private record TestTiming(String name, long durationMs) {
    }
}
