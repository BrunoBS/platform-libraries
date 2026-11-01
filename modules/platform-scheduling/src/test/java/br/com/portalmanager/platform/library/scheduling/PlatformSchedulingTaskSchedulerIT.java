package br.com.portalmanager.platform.library.scheduling;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;

import br.com.portalmanager.platform.library.redis.configuration.PlatformRedisAutoConfiguration;
import br.com.portalmanager.platform.library.scheduling.configuration.PlatformSchedulingAutoConfiguration;
import br.com.portalmanager.platform.library.scheduling.task.PlatformScheduleOptions;
import br.com.portalmanager.platform.library.scheduling.task.PlatformTaskScheduler;
import br.com.portalmanager.platform.library.testing.redis.RedisTestConfiguration;
import br.com.portalmanager.platform.library.testing.redis.annotation.WithRedis;
import java.time.Duration;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import jakarta.annotation.PostConstruct;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.data.redis.autoconfigure.DataRedisAutoConfiguration;
import org.springframework.boot.data.redis.autoconfigure.DataRedisReactiveAutoConfiguration;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.testcontainers.containers.GenericContainer;

@WithRedis
@ExtendWith(SpringExtension.class)
@ContextConfiguration(classes = RedisTestConfiguration.class)
class PlatformSchedulingTaskSchedulerIT {
    private static GenericContainer<?> redis;

    @BeforeAll
    static void setUpRedis(@org.springframework.beans.factory.annotation.Autowired GenericContainer<?> container) {
        redis = container;
    }

    @Test
    void programmaticTaskCanBeRegisteredFromPostConstruct() {
        PostConstructRegistrationProbe.reset();
        newApplicationContextRunner(PostConstructRegistrationApplication.class, "scheduling-postconstruct-it")
                .run(context -> {
                    assertThat(context).hasNotFailed();
                    await().atMost(Duration.ofSeconds(5))
                            .untilAsserted(() -> assertThat(PostConstructRegistrationProbe.executions()).isPositive());
                });
    }

    @Test
    void fixedRateFixedDelayAndCronTasksAreScheduledAutomatically() throws InterruptedException {
        TriggerProbe.reset();
        newApplicationContextRunner(TriggerApplication.class, "scheduling-trigger-modes-it")
                .run(context -> {
                    assertThat(context).hasNotFailed();
                    awaitTriggerModes();
                    assertThat(TriggerProbe.fixedRateExecutions()).isPositive();
                    assertThat(TriggerProbe.fixedDelayExecutions()).isPositive();
                    assertThat(TriggerProbe.cronExecutions()).isPositive();
                });
    }

    @Test
    void namedTaskSchedulersRemainRoutableThroughLockingDecorator() throws InterruptedException {
        NamedSchedulerProbe.reset();
        newApplicationContextRunner(NamedSchedulerApplication.class, "scheduling-named-schedulers-it")
                .run(context -> {
                    assertThat(context).hasNotFailed();
                    awaitNamedSchedulers();
                    assertThat(context.getBean("primaryScheduler", TrackingTaskScheduler.class)
                                    .fixedDelaySchedules())
                            .isPositive();
                    assertThat(context.getBean("secondaryScheduler", TrackingTaskScheduler.class)
                                    .fixedDelaySchedules())
                            .isPositive();
                    assertThat(NamedSchedulerProbe.primaryExecutions()).isPositive();
                    assertThat(NamedSchedulerProbe.secondaryExecutions()).isPositive();
                });
    }

    @Test
    void defaultRouterStopsScheduledTasksWhenApplicationContextCloses() throws InterruptedException {
        LifecycleProbe.reset();
        newApplicationContextRunner(LifecycleApplication.class, "scheduling-lifecycle-it")
                .run(context -> {
                    assertThat(context).hasNotFailed();
                    await().atMost(Duration.ofSeconds(3))
                            .untilAsserted(() -> assertThat(LifecycleProbe.executions()).isGreaterThanOrEqualTo(2));
                    ((ConfigurableApplicationContext) context).close();
                    int afterClose = LifecycleProbe.executions();
                    Thread.sleep(350L);
                    assertThat(LifecycleProbe.executions()).isEqualTo(afterClose);
                });
    }

    @Test
    void contextShutdownWaitsForInFlightTaskToReleaseItsRedisLock() throws InterruptedException {
        InFlightShutdownProbe.reset();
        newApplicationContextRunner(InFlightShutdownApplication.class, "scheduling-in-flight-shutdown-it")
                .run(context -> {
                    assertThat(context).hasNotFailed();
                    assertThat(InFlightShutdownProbe.started().await(3, TimeUnit.SECONDS)).isTrue();
                    ((ConfigurableApplicationContext) context).close();
                    assertThat(InFlightShutdownProbe.completed().getCount()).isZero();
                });
    }

    private static ApplicationContextRunner newApplicationContextRunner(Class<?> userConfiguration, String serviceName) {
        return new ApplicationContextRunner()
                .withConfiguration(AutoConfigurations.of(
                        DataRedisAutoConfiguration.class,
                        DataRedisReactiveAutoConfiguration.class,
                        PlatformRedisAutoConfiguration.class,
                        PlatformSchedulingAutoConfiguration.class))
                .withUserConfiguration(userConfiguration)
                .withPropertyValues(
                        "spring.application.name=" + serviceName,
                        "spring.data.redis.host=" + redis.getHost(),
                        "spring.data.redis.port=" + redis.getMappedPort(6379),
                        "spring.data.redis.timeout=1s",
                        "spring.data.redis.connect-timeout=1s",
                        "golden.scheduling.distributed-lock.lock-at-most-for=PT30S",
                        "golden.scheduling.distributed-lock.lock-at-least-for=PT0S",
                        "golden.scheduling.distributed-lock.keep-alive=false");
    }

    private static void awaitTriggerModes() throws InterruptedException {
        await().atMost(Duration.ofSeconds(6))
                .untilAsserted(() -> assertThat(TriggerProbe.allModesExecuted()).isTrue());
    }

    private static void awaitNamedSchedulers() throws InterruptedException {
        await().atMost(Duration.ofSeconds(5))
                .untilAsserted(() -> {
                    assertThat(NamedSchedulerProbe.primaryExecuted()).isTrue();
                    assertThat(NamedSchedulerProbe.secondaryExecuted()).isTrue();
                });
    }

    @Configuration(proxyBeanMethods = false)
    static class PostConstructRegistrationApplication {
        @Bean
        PostConstructRegistrationProbe postConstructRegistrationProbe(PlatformTaskScheduler scheduler) {
            return new PostConstructRegistrationProbe(scheduler);
        }
    }

    static class PostConstructRegistrationProbe {
        private static final AtomicInteger EXECUTIONS = new AtomicInteger();
        private final PlatformTaskScheduler scheduler;

        PostConstructRegistrationProbe(PlatformTaskScheduler scheduler) {
            this.scheduler = scheduler;
        }

        @PostConstruct
        void register() {
            scheduler.scheduleWithFixedDelay(
                    EXECUTIONS::incrementAndGet,
                    Duration.ofMillis(100),
                    PlatformScheduleOptions.withLock("postconstruct-registration"));
        }

        static void reset() {
            EXECUTIONS.set(0);
        }

        static int executions() {
            return EXECUTIONS.get();
        }
    }

    @Configuration(proxyBeanMethods = false)
    static class TriggerApplication {
        @Bean
        TriggerProbe triggerProbe() {
            return new TriggerProbe();
        }
    }

    static class TriggerProbe {
        private static final AtomicInteger FIXED_RATE = new AtomicInteger();
        private static final AtomicInteger FIXED_DELAY = new AtomicInteger();
        private static final AtomicInteger CRON = new AtomicInteger();

        @Scheduled(fixedRate = 100)
        void runAtFixedRate() {
            FIXED_RATE.incrementAndGet();
        }

        @Scheduled(fixedDelay = 100)
        void runAtFixedDelay() {
            FIXED_DELAY.incrementAndGet();
        }

        @Scheduled(cron = "*/1 * * * * *")
        void runWithCron() {
            CRON.incrementAndGet();
        }

        static void reset() {
            FIXED_RATE.set(0);
            FIXED_DELAY.set(0);
            CRON.set(0);
        }

        static int fixedRateExecutions() {
            return FIXED_RATE.get();
        }

        static int fixedDelayExecutions() {
            return FIXED_DELAY.get();
        }

        static int cronExecutions() {
            return CRON.get();
        }

        static boolean allModesExecuted() {
            return fixedRateExecutions() > 0 && fixedDelayExecutions() > 0 && cronExecutions() > 0;
        }
    }

    @Configuration(proxyBeanMethods = false)
    static class LifecycleApplication {
        @Bean
        LifecycleProbe lifecycleProbe() {
            return new LifecycleProbe();
        }
    }

    static class LifecycleProbe {
        private static final AtomicInteger EXECUTIONS = new AtomicInteger();

        @Scheduled(fixedDelay = 50)
        void run() {
            EXECUTIONS.incrementAndGet();
        }

        static void reset() {
            EXECUTIONS.set(0);
        }

        static int executions() {
            return EXECUTIONS.get();
        }
    }

    @Configuration(proxyBeanMethods = false)
    static class InFlightShutdownApplication {
        @Bean
        InFlightShutdownProbe inFlightShutdownProbe() {
            return new InFlightShutdownProbe();
        }
    }

    static class InFlightShutdownProbe {
        private static volatile CountDownLatch STARTED = new CountDownLatch(1);
        private static volatile CountDownLatch COMPLETED = new CountDownLatch(1);

        @Scheduled(fixedDelay = 100)
        void run() {
            STARTED.countDown();
            try {
                Thread.sleep(30_000L);
            } catch (InterruptedException interrupted) {
                Thread.currentThread().interrupt();
            } finally {
                COMPLETED.countDown();
            }
        }

        static void reset() {
            STARTED = new CountDownLatch(1);
            COMPLETED = new CountDownLatch(1);
        }

        static CountDownLatch started() {
            return STARTED;
        }

        static CountDownLatch completed() {
            return COMPLETED;
        }
    }

    @Configuration(proxyBeanMethods = false)
    static class NamedSchedulerApplication {
        @Bean("primaryScheduler")
        TrackingTaskScheduler primaryScheduler() {
            return new TrackingTaskScheduler();
        }

        @Bean("secondaryScheduler")
        TrackingTaskScheduler secondaryScheduler() {
            return new TrackingTaskScheduler();
        }

        @Bean
        NamedSchedulerProbe namedSchedulerProbe() {
            return new NamedSchedulerProbe();
        }
    }

    static class TrackingTaskScheduler extends ThreadPoolTaskScheduler {
        private final AtomicInteger FIXED_DELAY_SCHEDULES = new AtomicInteger();

        @Override
        public ScheduledFuture<?> scheduleWithFixedDelay(Runnable task, Duration delay) {
            FIXED_DELAY_SCHEDULES.incrementAndGet();
            return super.scheduleWithFixedDelay(task, delay);
        }

        @Override
        public ScheduledFuture<?> scheduleWithFixedDelay(Runnable task, java.time.Instant startTime, Duration delay) {
            FIXED_DELAY_SCHEDULES.incrementAndGet();
            return super.scheduleWithFixedDelay(task, startTime, delay);
        }

        int fixedDelaySchedules() {
            return FIXED_DELAY_SCHEDULES.get();
        }
    }

    static class NamedSchedulerProbe {
        private static final AtomicInteger PRIMARY = new AtomicInteger();
        private static final AtomicInteger SECONDARY = new AtomicInteger();

        @Scheduled(fixedDelay = 100, scheduler = "primaryScheduler")
        void runOnPrimary() {
            PRIMARY.incrementAndGet();
        }

        @Scheduled(fixedDelay = 100, scheduler = "secondaryScheduler")
        void runOnSecondary() {
            SECONDARY.incrementAndGet();
        }

        static void reset() {
            PRIMARY.set(0);
            SECONDARY.set(0);
        }

        static int primaryExecutions() {
            return PRIMARY.get();
        }

        static int secondaryExecutions() {
            return SECONDARY.get();
        }

        static boolean primaryExecuted() {
            return primaryExecutions() > 0;
        }

        static boolean secondaryExecuted() {
            return secondaryExecutions() > 0;
        }
    }
}
