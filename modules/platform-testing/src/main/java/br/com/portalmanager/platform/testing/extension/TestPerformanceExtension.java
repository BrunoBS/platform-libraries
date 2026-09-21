package br.com.portalmanager.platform.testing.extension;

import org.junit.jupiter.api.extension.AfterAllCallback;
import org.junit.jupiter.api.extension.AfterTestExecutionCallback;
import org.junit.jupiter.api.extension.BeforeAllCallback;
import org.junit.jupiter.api.extension.BeforeTestExecutionCallback;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Duration;
import java.time.Instant;
import java.util.Comparator;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.CopyOnWriteArrayList;

public final class TestPerformanceExtension implements
        BeforeAllCallback,
        BeforeTestExecutionCallback,
        AfterTestExecutionCallback,
        AfterAllCallback {

    private static final Logger log = LoggerFactory.getLogger(TestPerformanceExtension.class);
    private static final ExtensionContext.Namespace NAMESPACE =
            ExtensionContext.Namespace.create(TestPerformanceExtension.class);

    private static final String TEST_START = "test-start";

    private static final ConcurrentMap<Class<?>, ClassTiming> CLASS_TIMINGS =
            new ConcurrentHashMap<>();

    @Override
    public void beforeAll(ExtensionContext context) {
        CLASS_TIMINGS.put(
                context.getRequiredTestClass(),
                new ClassTiming(Instant.now(), new CopyOnWriteArrayList<>())
        );
    }

    @Override
    public void beforeTestExecution(ExtensionContext context) {
        context.getStore(NAMESPACE).put(TEST_START, Instant.now());
    }

    @Override
    public void afterTestExecution(ExtensionContext context) {
        Instant startedAt = context.getStore(NAMESPACE).remove(TEST_START, Instant.class);
        if (startedAt == null) {
            return;
        }

        long elapsedMs = Duration.between(startedAt, Instant.now()).toMillis();
        ClassTiming classTiming = CLASS_TIMINGS.get(context.getRequiredTestClass());

        if (classTiming != null) {
            classTiming.tests().add(new TestTiming(context.getDisplayName(), elapsedMs));
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
    public void afterAll(ExtensionContext context) {
        Class<?> testClass = context.getRequiredTestClass();
        ClassTiming timing = CLASS_TIMINGS.remove(testClass);
        if (timing == null) {
            return;
        }

        long totalMs = Duration.between(timing.startedAt(), Instant.now()).toMillis();
        List<TestTiming> tests = timing.tests();

        log.info(
                "[TEST-PERF] class={} total={}ms tests={}",
                testClass.getSimpleName(),
                totalMs,
                tests.size()
        );

        tests.stream()
                .sorted(Comparator.comparingLong(TestTiming::durationMs).reversed())
                .limit(5)
                .forEach(test -> log.info(
                        "[TEST-PERF] class={} slowest-test={} duration={}ms",
                        testClass.getSimpleName(),
                        test.name(),
                        test.durationMs()
                ));
    }

    private long slowThresholdMs() {
        return Long.getLong(
                "platform.testing.performance.slow-test-ms",
                2_000L
        );
    }

    private record ClassTiming(
            Instant startedAt,
            List<TestTiming> tests
    ) {
    }

    private record TestTiming(
            String name,
            long durationMs
    ) {
    }
}
