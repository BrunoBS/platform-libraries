package br.com.portalmanager.platform.library.scheduling;

import static org.assertj.core.api.Assertions.assertThat;

import br.com.portalmanager.platform.library.redis.configuration.PlatformRedisAutoConfiguration;
import br.com.portalmanager.platform.library.scheduling.configuration.PlatformSchedulingAutoConfiguration;
import br.com.portalmanager.platform.library.testing.redis.RedisTestConfiguration;
import br.com.portalmanager.platform.library.testing.redis.annotation.WithRedis;
import java.time.Duration;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.data.redis.autoconfigure.DataRedisAutoConfiguration;
import org.springframework.boot.data.redis.autoconfigure.DataRedisReactiveAutoConfiguration;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.testcontainers.containers.GenericContainer;

@WithRedis
@ExtendWith(SpringExtension.class)
@ContextConfiguration(classes = RedisTestConfiguration.class)
class PlatformSchedulingLockingIT {
    private static final String SERVICE_NAME = "platform-scheduling-it";
    private static final int TASK_DURATION_MILLIS = 1_200;
    private static GenericContainer<?> redis;

    @BeforeAll
    static void setUpRedis(
            @org.springframework.beans.factory.annotation.Autowired GenericContainer<?> container) {
        redis = container;
    }

    @Test
    void registeredScheduledRunnableIsMutuallyExclusiveAcrossServiceContexts() throws InterruptedException {
        ScheduledProbe.reset();
        ApplicationContextRunner runner = applicationContextRunner();

        runner.run(firstContext -> runner.run(secondContext -> {
            assertThat(firstContext).hasNotFailed();
            assertThat(secondContext).hasNotFailed();
            awaitAtLeastOneExecution();
            Thread.sleep(TASK_DURATION_MILLIS + 500L);

            assertThat(ScheduledProbe.maximumConcurrentExecutions()).isEqualTo(1);
            assertThat(ScheduledProbe.executions()).isPositive();
        }));
    }

    private static ApplicationContextRunner applicationContextRunner() {
        return new ApplicationContextRunner()
                .withConfiguration(AutoConfigurations.of(
                        DataRedisAutoConfiguration.class,
                        DataRedisReactiveAutoConfiguration.class,
                        PlatformRedisAutoConfiguration.class,
                        PlatformSchedulingAutoConfiguration.class))
                .withUserConfiguration(ScheduledApplication.class)
                .withPropertyValues(
                        "spring.application.name=" + SERVICE_NAME,
                        "spring.data.redis.host=" + redis.getHost(),
                        "spring.data.redis.port=" + redis.getMappedPort(6379),
                        "spring.data.redis.timeout=1s",
                        "spring.data.redis.connect-timeout=1s",
                        "golden.scheduling.distributed-lock.lock-at-most-for=PT30S",
                        "golden.scheduling.distributed-lock.lock-at-least-for=PT0S",
                        "golden.scheduling.distributed-lock.keep-alive=false");
    }

    private static void awaitAtLeastOneExecution() throws InterruptedException {
        long deadline = System.nanoTime() + Duration.ofSeconds(5).toNanos();
        while (ScheduledProbe.executions() == 0 && System.nanoTime() < deadline) {
            Thread.sleep(25L);
        }
        assertThat(ScheduledProbe.executions()).isPositive();
    }

    @Configuration(proxyBeanMethods = false)
    static class ScheduledApplication {
        @Bean
        ScheduledProbe scheduledProbe() {
            return new ScheduledProbe();
        }
    }

    static class ScheduledProbe {
        private static final AtomicInteger RUNNING = new AtomicInteger();
        private static final AtomicInteger MAXIMUM_RUNNING = new AtomicInteger();
        private static final AtomicInteger EXECUTIONS = new AtomicInteger();

        @Scheduled(fixedDelay = 100)
        void runPeriodically() {
            int concurrent = RUNNING.incrementAndGet();
            MAXIMUM_RUNNING.accumulateAndGet(concurrent, Math::max);
            EXECUTIONS.incrementAndGet();
            try {
                Thread.sleep(TASK_DURATION_MILLIS);
            } catch (InterruptedException interrupted) {
                Thread.currentThread().interrupt();
            } finally {
                RUNNING.decrementAndGet();
            }
        }

        static void reset() {
            RUNNING.set(0);
            MAXIMUM_RUNNING.set(0);
            EXECUTIONS.set(0);
        }

        static int maximumConcurrentExecutions() {
            return MAXIMUM_RUNNING.get();
        }

        static int executions() {
            return EXECUTIONS.get();
        }
    }
}
