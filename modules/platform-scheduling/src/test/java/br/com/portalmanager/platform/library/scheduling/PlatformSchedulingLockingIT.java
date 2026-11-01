package br.com.portalmanager.platform.library.scheduling;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;

import br.com.portalmanager.platform.library.redis.configuration.PlatformRedisAutoConfiguration;
import br.com.portalmanager.platform.library.scheduling.annotation.DisableScheduledLock;
import br.com.portalmanager.platform.library.scheduling.configuration.PlatformSchedulingAutoConfiguration;
import br.com.portalmanager.platform.library.scheduling.task.PlatformScheduleOptions;
import br.com.portalmanager.platform.library.scheduling.task.PlatformTaskScheduler;
import br.com.portalmanager.platform.library.testing.redis.RedisTestConfiguration;
import br.com.portalmanager.platform.library.testing.redis.annotation.WithRedis;
import java.time.Duration;
import java.time.Instant;
import java.util.Optional;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import net.javacrumbs.shedlock.core.LockConfiguration;
import net.javacrumbs.shedlock.core.LockProvider;
import net.javacrumbs.shedlock.core.SimpleLock;
import org.aopalliance.intercept.MethodInterceptor;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.aop.framework.ProxyFactory;
import org.springframework.beans.factory.SmartInitializingSingleton;
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
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

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
        ApplicationContextRunner runner = applicationContextRunner(ScheduledApplication.class, SERVICE_NAME, false, "PT30S");

        runner.run(firstContext -> runner.run(secondContext -> {
            assertThat(firstContext).hasNotFailed();
            assertThat(secondContext).hasNotFailed();
            awaitAtLeastOneExecution();
            Thread.sleep(TASK_DURATION_MILLIS + 500L);

            assertThat(ScheduledProbe.maximumConcurrentExecutions()).isEqualTo(1);
            assertThat(ScheduledProbe.executions()).isPositive();
        }));
    }

    @Test
    void reactiveMonoAndFluxKeepTheLockUntilTheirPublishersComplete() throws InterruptedException {
        ReactiveScheduledProbe.reset();
        ApplicationContextRunner runner = applicationContextRunner(
                ReactiveScheduledApplication.class, SERVICE_NAME, false, "PT30S");

        runner.run(firstContext -> runner.run(secondContext -> {
            assertThat(firstContext).hasNotFailed();
            assertThat(secondContext).hasNotFailed();
            awaitReactiveExecution();
            Thread.sleep(Duration.ofMillis(1_500).toMillis());

            assertThat(ReactiveScheduledProbe.monoMaximumConcurrency()).isEqualTo(1);
            assertThat(ReactiveScheduledProbe.fluxMaximumConcurrency()).isEqualTo(1);
            assertThat(ReactiveScheduledProbe.monoExecutions()).isPositive();
            assertThat(ReactiveScheduledProbe.fluxExecutions()).isPositive();
        }));
    }

    @Test
    void disabledLockAnnotationAllowsTwoInstancesToRunTheSameScheduledTask() throws InterruptedException {
        ExcludedLockScheduledProbe.reset();
        ApplicationContextRunner runner = applicationContextRunner(
                ExcludedLockScheduledApplication.class, SERVICE_NAME, false, "PT30S");

        runner.run(firstContext -> runner.run(secondContext -> {
            assertThat(firstContext).hasNotFailed();
            assertThat(secondContext).hasNotFailed();
            awaitExcludedTaskConcurrency();

            assertThat(ExcludedLockScheduledProbe.maximumConcurrentExecutions()).isEqualTo(2);
        }));
    }

    @Test
    void differentServicesUseIndependentLockNamespaces() throws InterruptedException {
        ScheduledProbe.reset();
        ApplicationContextRunner firstService = applicationContextRunner(
                ScheduledApplication.class, "scheduling-namespace-a", false, "PT30S");
        ApplicationContextRunner secondService = applicationContextRunner(
                ScheduledApplication.class, "scheduling-namespace-b", false, "PT30S");

        firstService.run(firstContext -> secondService.run(secondContext -> {
            assertThat(firstContext).hasNotFailed();
            assertThat(secondContext).hasNotFailed();
            awaitConcurrentExecutions(2, Duration.ofSeconds(5));

            assertThat(ScheduledProbe.maximumConcurrentExecutions()).isEqualTo(2);
        }));
    }

    @Test
    void cglibProxiesKeepAStableLockIdentityAcrossServiceContexts() throws InterruptedException {
        ProxyScheduledProbe.reset();
        ApplicationContextRunner runner = applicationContextRunner(
                ProxiedScheduledApplication.class, SERVICE_NAME, false, "PT30S");

        runner.run(firstContext -> runner.run(secondContext -> {
            assertThat(firstContext).hasNotFailed();
            assertThat(secondContext).hasNotFailed();
            awaitProxyExecution();
            Thread.sleep(TASK_DURATION_MILLIS + 500L);

            assertThat(ProxyScheduledProbe.maximumConcurrentExecutions()).isEqualTo(1);
            assertThat(ProxyScheduledProbe.executions()).isPositive();
        }));
    }

    @Test
    void cronTasksAreMutuallyExclusiveAcrossServiceContexts() throws InterruptedException {
        CronScheduledProbe.reset();
        ApplicationContextRunner runner = applicationContextRunner(
                CronScheduledApplication.class, SERVICE_NAME, false, "PT30S");

        runner.run(firstContext -> runner.run(secondContext -> {
            assertThat(firstContext).hasNotFailed();
            assertThat(secondContext).hasNotFailed();
            awaitCronExecution();
            Thread.sleep(Duration.ofSeconds(3).toMillis());

            assertThat(CronScheduledProbe.maximumConcurrentExecutions()).isEqualTo(1);
            assertThat(CronScheduledProbe.executions()).isPositive();
        }));
    }

    @Test
    void repeatedSchedulesOnOneMethodShareTheSameLockIdentity() throws InterruptedException {
        RepeatedScheduledProbe.reset();
        applicationContextRunner(RepeatedScheduledApplication.class, SERVICE_NAME, false, "PT30S")
                .run(context -> {
                    assertThat(context).hasNotFailed();
                    Thread.sleep(Duration.ofSeconds(2).toMillis());

                    assertThat(RepeatedScheduledProbe.maximumConcurrentExecutions()).isEqualTo(1);
                    assertThat(RepeatedScheduledProbe.executions()).isPositive();
                });
    }

    @Test
    void failsClosedDuringRealRedisOutageAndResumesAfterRedisRecovers() throws Exception {
        OutageScheduledProbe.reset();
        ApplicationContextRunner runner = applicationContextRunner(
                OutageScheduledApplication.class, SERVICE_NAME, false, "PT30S");

        runner.run(firstContext -> runner.run(secondContext -> {
            assertThat(firstContext).hasNotFailed();
            assertThat(secondContext).hasNotFailed();
            assertThat(OutageScheduledProbe.started().await(5, TimeUnit.SECONDS)).isTrue();

            redis.execInContainer("redis-cli", "CLIENT", "PAUSE", "2500", "ALL");
            Thread.sleep(1_500L);
            assertThat(OutageScheduledProbe.executions()).isEqualTo(1);

            assertThat(OutageScheduledProbe.secondExecution().await(12, TimeUnit.SECONDS)).isTrue();
            assertThat(OutageScheduledProbe.maximumConcurrentExecutions()).isEqualTo(1);
        }));
    }

    @Test
    void failsStartupWhenTheRealRedisContainerIsUnavailable() {
        ApplicationContextRunner runner = applicationContextRunner(
                ScheduledApplication.class, SERVICE_NAME, false, "PT30S");
        redis.stop();
        try {
            runner.run(context -> {
                assertThat(context).hasFailed();
                assertThat(context.getStartupFailure())
                        .hasStackTraceContaining("Distributed scheduling lock Redis is unavailable");
            });
        } finally {
            redis.start();
        }
    }

    @Test
    void anAbandonedLeaseCanBeAcquiredAgainAfterItsExpiry() throws InterruptedException {
        ApplicationContextRunner runner = applicationContextRunner(
                ScheduledApplication.class, SERVICE_NAME, false, "PT30S");
        runner.run(firstContext -> runner.run(secondContext -> {
            assertThat(firstContext).hasNotFailed();
            assertThat(secondContext).hasNotFailed();
            LockProvider firstProvider = firstContext.getBean(LockProvider.class);
            LockProvider secondProvider = secondContext.getBean(LockProvider.class);
            String abandonedLockName = SERVICE_NAME + ":simulated-crashed-worker";
            assertThat(firstProvider.lock(new LockConfiguration(
                            Instant.now(), abandonedLockName, Duration.ofSeconds(2), Duration.ZERO)))
                    .isPresent();
            assertThat(secondProvider.lock(new LockConfiguration(
                            Instant.now(), abandonedLockName, Duration.ofSeconds(2), Duration.ZERO)))
                    .isEmpty();

            Thread.sleep(Duration.ofMillis(2_500).toMillis());

            Optional<SimpleLock> recoveredLock = secondProvider.lock(new LockConfiguration(
                    Instant.now(), abandonedLockName, Duration.ofSeconds(2), Duration.ZERO));
            assertThat(recoveredLock).isPresent();
            recoveredLock.orElseThrow().unlock();
        }));
    }

    @Test
    void publicProgrammaticSchedulerCoordinatesTheSameTaskAcrossContexts() throws InterruptedException {
        ProgrammaticScheduledProbe.reset();
        ApplicationContextRunner runner = applicationContextRunner(
                ProgrammaticScheduledApplication.class, SERVICE_NAME, false, "PT30S");

        runner.run(firstContext -> runner.run(secondContext -> {
            assertThat(firstContext).hasNotFailed();
            assertThat(secondContext).hasNotFailed();
            awaitProgrammaticExecution();
            Thread.sleep(TASK_DURATION_MILLIS + 500L);

            assertThat(ProgrammaticScheduledProbe.maximumConcurrentExecutions()).isEqualTo(1);
            assertThat(ProgrammaticScheduledProbe.executions()).isPositive();
        }));
    }

    @Test
    void programmaticSchedulerCanSkipTheLockForOneTaskAcrossContexts() throws InterruptedException {
        ProgrammaticScheduledProbe.reset();
        ApplicationContextRunner runner = applicationContextRunner(
                UnlockedProgrammaticScheduledApplication.class, SERVICE_NAME, false, "PT30S");

        runner.run(firstContext -> {
            assertThat(firstContext).hasNotFailed();
            awaitProgrammaticExecution();
            runner.run(secondContext -> {
                assertThat(secondContext).hasNotFailed();
                assertThat(ProgrammaticScheduledProbe.concurrentExecution().await(3, TimeUnit.SECONDS)).isTrue();
                assertThat(ProgrammaticScheduledProbe.maximumConcurrentExecutions()).isGreaterThan(1);
            });
        });
    }

    @Test
    void keepAliveRenewsLeaseWhileScheduledTaskRunsLongerThanItsInitialTtl() throws InterruptedException {
        LongRunningScheduledProbe.reset();
        ApplicationContextRunner runner = applicationContextRunner(
                LongRunningApplication.class, SERVICE_NAME, true, "PT30S");

        runner.run(firstContext -> runner.run(secondContext -> {
            assertThat(firstContext).hasNotFailed();
            assertThat(secondContext).hasNotFailed();
            awaitLongRunningExecution();
            Thread.sleep(Duration.ofSeconds(31).toMillis());

            assertThat(LongRunningScheduledProbe.maximumConcurrentExecutions()).isEqualTo(1);
            assertThat(LongRunningScheduledProbe.executions()).isEqualTo(1);
            secondContext.close();
            awaitLongRunningTaskCompletion();
            assertThat(LongRunningScheduledProbe.executions()).isEqualTo(1);
        }));
    }

    private static ApplicationContextRunner applicationContextRunner(
            Class<?> scheduledApplication, String serviceName, boolean keepAlive, String lockAtMostFor) {
        return new ApplicationContextRunner()
                .withConfiguration(AutoConfigurations.of(
                        DataRedisAutoConfiguration.class,
                        DataRedisReactiveAutoConfiguration.class,
                        PlatformRedisAutoConfiguration.class,
                        PlatformSchedulingAutoConfiguration.class))
                .withUserConfiguration(scheduledApplication)
                .withPropertyValues(
                        "spring.application.name=" + serviceName,
                        "spring.data.redis.host=" + redis.getHost(),
                        "spring.data.redis.port=" + redis.getMappedPort(6379),
                        "spring.data.redis.timeout=1s",
                        "spring.data.redis.connect-timeout=1s",
                        "golden.scheduling.distributed-lock.lock-at-most-for=" + lockAtMostFor,
                        "golden.scheduling.distributed-lock.lock-at-least-for=PT0S",
                        "golden.scheduling.distributed-lock.keep-alive=" + keepAlive);
    }

    private static void awaitAtLeastOneExecution() throws InterruptedException {
        await().atMost(Duration.ofSeconds(5))
                .untilAsserted(() -> assertThat(ScheduledProbe.executions()).isPositive());
    }

    private static void awaitConcurrentExecutions(int expected, Duration timeout) throws InterruptedException {
        await().atMost(timeout)
                .untilAsserted(() -> assertThat(ScheduledProbe.maximumConcurrentExecutions())
                        .isGreaterThanOrEqualTo(expected));
    }

    private static void awaitLongRunningExecution() throws InterruptedException {
        await().atMost(Duration.ofSeconds(5))
                .untilAsserted(() -> assertThat(LongRunningScheduledProbe.executions()).isPositive());
    }

    private static void awaitLongRunningTaskCompletion() throws InterruptedException {
        await().atMost(Duration.ofSeconds(15))
                .untilAsserted(() -> assertThat(LongRunningScheduledProbe.isRunning()).isFalse());
    }

    private static void awaitProxyExecution() throws InterruptedException {
        await().atMost(Duration.ofSeconds(5))
                .untilAsserted(() -> assertThat(ProxyScheduledProbe.executions()).isPositive());
    }

    private static void awaitCronExecution() throws InterruptedException {
        await().atMost(Duration.ofSeconds(5))
                .untilAsserted(() -> assertThat(CronScheduledProbe.executions()).isPositive());
    }

    private static void awaitProgrammaticExecution() throws InterruptedException {
        await().atMost(Duration.ofSeconds(5))
                .untilAsserted(() -> assertThat(ProgrammaticScheduledProbe.executions()).isPositive());
    }

    private static void awaitReactiveExecution() throws InterruptedException {
        await().atMost(Duration.ofSeconds(8))
                .untilAsserted(() -> {
                    assertThat(ReactiveScheduledProbe.hasMonoExecuted()).isTrue();
                    assertThat(ReactiveScheduledProbe.hasFluxExecuted()).isTrue();
                });
    }

    private static void awaitExcludedTaskConcurrency() throws InterruptedException {
        await().atMost(Duration.ofSeconds(5))
                .untilAsserted(() -> assertThat(ExcludedLockScheduledProbe.maximumConcurrentExecutions())
                        .isGreaterThanOrEqualTo(2));
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

    @Configuration(proxyBeanMethods = false)
    static class ReactiveScheduledApplication {
        @Bean
        ReactiveScheduledProbe reactiveScheduledProbe() {
            return new ReactiveScheduledProbe();
        }
    }

    static class ReactiveScheduledProbe {
        private static final AtomicInteger MONO_RUNNING = new AtomicInteger();
        private static final AtomicInteger MONO_MAXIMUM = new AtomicInteger();
        private static final AtomicInteger MONO_EXECUTIONS = new AtomicInteger();
        private static final AtomicInteger FLUX_RUNNING = new AtomicInteger();
        private static final AtomicInteger FLUX_MAXIMUM = new AtomicInteger();
        private static final AtomicInteger FLUX_EXECUTIONS = new AtomicInteger();

        @Scheduled(fixedRate = 100)
        Mono<Void> runMono() {
            return Mono.defer(() -> {
                enter(MONO_RUNNING, MONO_MAXIMUM, MONO_EXECUTIONS);
                return Mono.delay(Duration.ofMillis(1_200)).doFinally(signal -> MONO_RUNNING.decrementAndGet()).then();
            });
        }

        @Scheduled(fixedRate = 100)
        Flux<Long> runFlux() {
            return Flux.defer(() -> {
                enter(FLUX_RUNNING, FLUX_MAXIMUM, FLUX_EXECUTIONS);
                return Flux.interval(Duration.ofMillis(150))
                        .take(8)
                        .doFinally(signal -> FLUX_RUNNING.decrementAndGet());
            });
        }

        private static void enter(AtomicInteger running, AtomicInteger maximum, AtomicInteger executions) {
            int concurrent = running.incrementAndGet();
            maximum.accumulateAndGet(concurrent, Math::max);
            executions.incrementAndGet();
        }

        static void reset() {
            MONO_RUNNING.set(0);
            MONO_MAXIMUM.set(0);
            MONO_EXECUTIONS.set(0);
            FLUX_RUNNING.set(0);
            FLUX_MAXIMUM.set(0);
            FLUX_EXECUTIONS.set(0);
        }

        static boolean hasMonoExecuted() {
            return MONO_EXECUTIONS.get() > 0;
        }

        static boolean hasFluxExecuted() {
            return FLUX_EXECUTIONS.get() > 0;
        }

        static int monoExecutions() {
            return MONO_EXECUTIONS.get();
        }

        static int fluxExecutions() {
            return FLUX_EXECUTIONS.get();
        }

        static int monoMaximumConcurrency() {
            return MONO_MAXIMUM.get();
        }

        static int fluxMaximumConcurrency() {
            return FLUX_MAXIMUM.get();
        }
    }

    @Configuration(proxyBeanMethods = false)
    static class ExcludedLockScheduledApplication {
        @Bean
        ExcludedLockScheduledProbe excludedLockScheduledProbe() {
            return new ExcludedLockScheduledProbe();
        }
    }

    static class ExcludedLockScheduledProbe {
        private static final AtomicInteger RUNNING = new AtomicInteger();
        private static final AtomicInteger MAXIMUM_RUNNING = new AtomicInteger();

        @DisableScheduledLock
        @Scheduled(fixedRate = 100)
        void runWithoutLock() {
            int concurrent = RUNNING.incrementAndGet();
            MAXIMUM_RUNNING.accumulateAndGet(concurrent, Math::max);
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
        }

        static int maximumConcurrentExecutions() {
            return MAXIMUM_RUNNING.get();
        }
    }

    @Configuration(proxyBeanMethods = false)
    static class LongRunningApplication {
        @Bean
        LongRunningScheduledProbe longRunningScheduledProbe() {
            return new LongRunningScheduledProbe();
        }
    }

    static class LongRunningScheduledProbe {
        private static final long TASK_DURATION_MILLIS = Duration.ofSeconds(40).toMillis();
        private static final AtomicInteger RUNNING = new AtomicInteger();
        private static final AtomicInteger MAXIMUM_RUNNING = new AtomicInteger();
        private static final AtomicInteger EXECUTIONS = new AtomicInteger();

        @Scheduled(fixedDelay = 600_000)
        void runLongTask() {
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

        static boolean isRunning() {
            return RUNNING.get() > 0;
        }
    }

    @Configuration(proxyBeanMethods = false)
    static class CronScheduledApplication {
        @Bean
        CronScheduledProbe cronScheduledProbe() {
            return new CronScheduledProbe();
        }
    }

    static class CronScheduledProbe {
        private static final AtomicInteger RUNNING = new AtomicInteger();
        private static final AtomicInteger MAXIMUM_RUNNING = new AtomicInteger();
        private static final AtomicInteger EXECUTIONS = new AtomicInteger();

        @Scheduled(cron = "*/1 * * * * *")
        void runWithCron() {
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

        static int executions() {
            return EXECUTIONS.get();
        }

        static int maximumConcurrentExecutions() {
            return MAXIMUM_RUNNING.get();
        }
    }

    @Configuration(proxyBeanMethods = false)
    static class RepeatedScheduledApplication {
        @Bean
        org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler taskScheduler() {
            org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler scheduler =
                    new org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler();
            scheduler.setPoolSize(4);
            scheduler.setThreadNamePrefix("repeated-schedules-it-");
            return scheduler;
        }

        @Bean
        RepeatedScheduledProbe repeatedScheduledProbe() {
            return new RepeatedScheduledProbe();
        }
    }

    static class RepeatedScheduledProbe {
        private static final AtomicInteger RUNNING = new AtomicInteger();
        private static final AtomicInteger MAXIMUM_RUNNING = new AtomicInteger();
        private static final AtomicInteger EXECUTIONS = new AtomicInteger();

        @org.springframework.scheduling.annotation.Schedules({
            @Scheduled(fixedRate = 100), @Scheduled(fixedDelay = 100)
        })
        void runRepeatedly() {
            int concurrent = RUNNING.incrementAndGet();
            MAXIMUM_RUNNING.accumulateAndGet(concurrent, Math::max);
            EXECUTIONS.incrementAndGet();
            try {
                Thread.sleep(350L);
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

        static int executions() {
            return EXECUTIONS.get();
        }

        static int maximumConcurrentExecutions() {
            return MAXIMUM_RUNNING.get();
        }
    }

    @Configuration(proxyBeanMethods = false)
    static class OutageScheduledApplication {
        @Bean
        OutageScheduledProbe outageScheduledProbe() {
            return new OutageScheduledProbe();
        }
    }

    static class OutageScheduledProbe {
        private static final AtomicInteger RUNNING = new AtomicInteger();
        private static final AtomicInteger MAXIMUM_RUNNING = new AtomicInteger();
        private static final AtomicInteger EXECUTIONS = new AtomicInteger();
        private static volatile CountDownLatch STARTED = new CountDownLatch(1);
        private static volatile CountDownLatch SECOND_EXECUTION = new CountDownLatch(1);

        @Scheduled(fixedDelay = 100)
        void runPeriodically() {
            int execution = EXECUTIONS.incrementAndGet();
            int concurrent = RUNNING.incrementAndGet();
            MAXIMUM_RUNNING.accumulateAndGet(concurrent, Math::max);
            STARTED.countDown();
            if (execution > 1) {
                SECOND_EXECUTION.countDown();
            }
            try {
                Thread.sleep(8_000L);
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
            STARTED = new CountDownLatch(1);
            SECOND_EXECUTION = new CountDownLatch(1);
        }

        static CountDownLatch started() {
            return STARTED;
        }

        static CountDownLatch secondExecution() {
            return SECOND_EXECUTION;
        }

        static int executions() {
            return EXECUTIONS.get();
        }

        static int maximumConcurrentExecutions() {
            return MAXIMUM_RUNNING.get();
        }
    }

    @Configuration(proxyBeanMethods = false)
    static class ProgrammaticScheduledApplication {
        @Bean
        SmartInitializingSingleton registerProgrammaticTask(PlatformTaskScheduler scheduler) {
            return () -> scheduler.scheduleWithFixedDelay(
                    ProgrammaticScheduledProbe::runTask,
                    Duration.ofMillis(100),
                    PlatformScheduleOptions.withLock("programmatic-probe"));
        }
    }

    @Configuration(proxyBeanMethods = false)
    static class UnlockedProgrammaticScheduledApplication {
        @Bean
        SmartInitializingSingleton registerProgrammaticTask(PlatformTaskScheduler scheduler) {
            return () -> scheduler.scheduleWithFixedDelay(
                    ProgrammaticScheduledProbe::runTask,
                    Duration.ofMillis(100),
                    PlatformScheduleOptions.withoutLock());
        }
    }

    static class ProgrammaticScheduledProbe {
        private static final AtomicInteger RUNNING = new AtomicInteger();
        private static final AtomicInteger MAXIMUM_RUNNING = new AtomicInteger();
        private static final AtomicInteger EXECUTIONS = new AtomicInteger();
        private static CountDownLatch CONCURRENT_EXECUTION = new CountDownLatch(1);

        static void runTask() {
            int concurrent = RUNNING.incrementAndGet();
            MAXIMUM_RUNNING.accumulateAndGet(concurrent, Math::max);
            if (concurrent > 1) {
                CONCURRENT_EXECUTION.countDown();
            }
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
            CONCURRENT_EXECUTION = new CountDownLatch(1);
        }

        static int executions() {
            return EXECUTIONS.get();
        }

        static int maximumConcurrentExecutions() {
            return MAXIMUM_RUNNING.get();
        }

        static CountDownLatch concurrentExecution() {
            return CONCURRENT_EXECUTION;
        }
    }

    @Configuration(proxyBeanMethods = false)
    static class ProxiedScheduledApplication {
        @Bean
        ProxyScheduledProbe proxiedScheduledProbe() {
            ProxyFactory proxyFactory = new ProxyFactory(new ProxyScheduledProbe());
            proxyFactory.setProxyTargetClass(true);
            proxyFactory.addAdvice((MethodInterceptor) invocation -> invocation.proceed());
            return (ProxyScheduledProbe) proxyFactory.getProxy();
        }
    }

    static class ProxyScheduledProbe {
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
