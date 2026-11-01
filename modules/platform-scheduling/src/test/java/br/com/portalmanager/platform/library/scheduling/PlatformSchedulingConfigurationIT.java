package br.com.portalmanager.platform.library.scheduling;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import br.com.portalmanager.platform.library.scheduling.configuration.PlatformSchedulingAutoConfiguration;
import java.time.Duration;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.RedisConnection;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.RedisConnectionFailureException;
import org.springframework.scheduling.annotation.Scheduled;

class PlatformSchedulingConfigurationIT {
    @Test
    void disabledSchedulingRunsWithoutRedis() throws InterruptedException {
        DisabledScheduledProbe.reset();
        new ApplicationContextRunner()
                .withConfiguration(AutoConfigurations.of(PlatformSchedulingAutoConfiguration.class))
                .withUserConfiguration(DisabledScheduledApplication.class)
                .withPropertyValues(
                        "spring.application.name=scheduling-disabled-it",
                        "golden.scheduling.distributed-lock.enabled=false")
                .run(context -> {
                    assertThat(context).hasNotFailed();
                    awaitDisabledTaskExecution();
                    assertThat(DisabledScheduledProbe.executions()).isPositive();
                });
    }

    @Test
    void enabledSchedulingFailsStartupWhenRedisIsUnavailable() {
        new ApplicationContextRunner()
                .withConfiguration(AutoConfigurations.of(PlatformSchedulingAutoConfiguration.class))
                .withUserConfiguration(UnavailableRedisConfiguration.class)
                .withPropertyValues("spring.application.name=scheduling-redis-startup-it")
                .run(context -> {
                    assertThat(context).hasFailed();
                    assertThat(context.getStartupFailure())
                            .hasStackTraceContaining("Distributed scheduling lock Redis is unavailable");
                });
    }

    @Test
    void rejectsKeepAliveWithLeaseShorterThanThirtySeconds() {
        new ApplicationContextRunner()
                .withConfiguration(AutoConfigurations.of(PlatformSchedulingAutoConfiguration.class))
                .withUserConfiguration(AvailableRedisConfiguration.class)
                .withPropertyValues(
                        "spring.application.name=scheduling-invalid-configuration-it",
                        "golden.scheduling.distributed-lock.keep-alive=true",
                        "golden.scheduling.distributed-lock.lock-at-most-for=PT29S")
                .run(context -> {
                    assertThat(context).hasFailed();
                    assertThat(context.getStartupFailure())
                            .hasStackTraceContaining("lock durations are invalid");
                });
    }

    private static void awaitDisabledTaskExecution() throws InterruptedException {
        await().atMost(Duration.ofSeconds(5))
                .untilAsserted(() -> assertThat(DisabledScheduledProbe.executions()).isPositive());
    }

    @Configuration(proxyBeanMethods = false)
    static class DisabledScheduledApplication {
        @Bean
        DisabledScheduledProbe disabledScheduledProbe() {
            return new DisabledScheduledProbe();
        }
    }

    static class DisabledScheduledProbe {
        private static final AtomicInteger EXECUTIONS = new AtomicInteger();

        @Scheduled(fixedDelay = 50)
        void runPeriodically() {
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
    static class UnavailableRedisConfiguration {
        @Bean
        RedisConnectionFactory redisConnectionFactory() {
            RedisConnectionFactory factory = mock(RedisConnectionFactory.class);
            RedisConnection connection = mock(RedisConnection.class);
            when(factory.getConnection()).thenReturn(connection);
            when(connection.ping()).thenThrow(new RedisConnectionFailureException("Redis unavailable"));
            return factory;
        }
    }

    @Configuration(proxyBeanMethods = false)
    static class AvailableRedisConfiguration {
        @Bean
        RedisConnectionFactory redisConnectionFactory() {
            RedisConnectionFactory factory = mock(RedisConnectionFactory.class);
            RedisConnection connection = mock(RedisConnection.class);
            when(factory.getConnection()).thenReturn(connection);
            when(connection.ping()).thenReturn("PONG");
            return factory;
        }
    }
}
