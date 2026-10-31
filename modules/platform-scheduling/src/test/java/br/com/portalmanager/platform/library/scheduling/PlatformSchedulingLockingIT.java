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
import org.aopalliance.intercept.MethodInterceptor;
import org.springframework.aop.framework.ProxyFactory;
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
        long deadline = System.nanoTime() + Duration.ofSeconds(5).toNanos();
        while (ScheduledProbe.executions() == 0 && System.nanoTime() < deadline) {
            Thread.sleep(25L);
        }
        assertThat(ScheduledProbe.executions()).isPositive();
    }

    private static void awaitConcurrentExecutions(int expected, Duration timeout) throws InterruptedException {
        long deadline = System.nanoTime() + timeout.toNanos();
        while (ScheduledProbe.maximumConcurrentExecutions() < expected && System.nanoTime() < deadline) {
            Thread.sleep(25L);
        }
        assertThat(ScheduledProbe.maximumConcurrentExecutions()).isGreaterThanOrEqualTo(expected);
    }

    private static void awaitLongRunningExecution() throws InterruptedException {
        long deadline = System.nanoTime() + Duration.ofSeconds(5).toNanos();
        while (LongRunningScheduledProbe.executions() == 0 && System.nanoTime() < deadline) {
            Thread.sleep(25L);
        }
        assertThat(LongRunningScheduledProbe.executions()).isPositive();
    }

    private static void awaitLongRunningTaskCompletion() throws InterruptedException {
        long deadline = System.nanoTime() + Duration.ofSeconds(15).toNanos();
        while (LongRunningScheduledProbe.isRunning() && System.nanoTime() < deadline) {
            Thread.sleep(25L);
        }
        assertThat(LongRunningScheduledProbe.isRunning()).isFalse();
    }

    private static void awaitProxyExecution() throws InterruptedException {
        long deadline = System.nanoTime() + Duration.ofSeconds(5).toNanos();
        while (ProxyScheduledProbe.executions() == 0 && System.nanoTime() < deadline) {
            Thread.sleep(25L);
        }
        assertThat(ProxyScheduledProbe.executions()).isPositive();
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
