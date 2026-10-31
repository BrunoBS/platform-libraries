package br.com.portalmanager.platform.library.scheduling.configuration;

import br.com.portalmanager.platform.library.messaging.exception.PlatformConfigurationException;
import br.com.portalmanager.platform.library.messaging.model.PlatformErrorDefinition;
import br.com.portalmanager.platform.library.scheduling.internal.LockingTaskScheduler;
import br.com.portalmanager.platform.library.scheduling.metrics.SchedulingLockMetrics;
import io.micrometer.core.instrument.MeterRegistry;
import java.time.Duration;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import net.javacrumbs.shedlock.core.ExtensibleLockProvider;
import net.javacrumbs.shedlock.core.LockProvider;
import net.javacrumbs.shedlock.provider.redis.spring.RedisLockProvider;
import net.javacrumbs.shedlock.support.KeepAliveLockProvider;
import org.springframework.beans.factory.BeanFactory;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.data.redis.connection.RedisConnection;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.scheduling.TaskScheduler;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.SchedulingConfigurer;
import org.springframework.scheduling.config.ScheduledTaskRegistrar;
import org.springframework.scheduling.config.TaskSchedulerRouter;
import org.slf4j.LoggerFactory;

@AutoConfiguration
@EnableScheduling
@EnableConfigurationProperties(PlatformSchedulingProperties.class)
@Import(PlatformSchedulingEnabledConfiguration.class)
public class PlatformSchedulingAutoConfiguration {
    @Bean
    @ConditionalOnProperty(
            prefix = "golden.scheduling.distributed-lock", name = "enabled", havingValue = "false")
    org.springframework.beans.factory.SmartInitializingSingleton platformSchedulingBypassWarning() {
        return () -> LoggerFactory.getLogger(PlatformSchedulingAutoConfiguration.class)
                .atWarn()
                .addKeyValue("errorCode", "PLT-SCHED-001")
                .log("Distributed scheduled lock disabled; scheduled tasks run without coordination");
    }

    @Bean
    @ConditionalOnProperty(
            prefix = "golden.scheduling.distributed-lock", name = "enabled", havingValue = "true", matchIfMissing = true)
    LockProvider platformSchedulingLockProvider(
            RedisConnectionFactory connectionFactory,
            PlatformSchedulingProperties properties,
            org.springframework.core.env.Environment environment,
            ObjectProvider<PlatformSchedulingLockExtensionScheduler> extensionScheduler) {
        String serviceName = requireServiceName(environment);
        validate(properties);
        verifyRedis(connectionFactory);
        ExtensibleLockProvider provider = new RedisLockProvider(connectionFactory, serviceName);
        if (!properties.isKeepAlive()) {
            return provider;
        }
        return new KeepAliveLockProvider(provider, extensionScheduler.getObject().executor());
    }

    @Bean(destroyMethod = "shutdown")
    @ConditionalOnProperty(
            prefix = "golden.scheduling.distributed-lock", name = "enabled", havingValue = "true", matchIfMissing = true)
    @ConditionalOnProperty(
            prefix = "golden.scheduling.distributed-lock", name = "keep-alive", havingValue = "true", matchIfMissing = true)
    PlatformSchedulingLockExtensionScheduler platformSchedulingLockExtensionScheduler() {
        return new PlatformSchedulingLockExtensionScheduler(
                Executors.newSingleThreadScheduledExecutor(task -> {
                    Thread thread = new Thread(task, "platform-scheduling-lock-extension");
                    thread.setDaemon(true);
                    return thread;
                }));
    }

    @Bean
    @ConditionalOnProperty(
            prefix = "golden.scheduling.distributed-lock", name = "enabled", havingValue = "true", matchIfMissing = true)
    SchedulingLockMetrics platformSchedulingLockMetrics(ObjectProvider<MeterRegistry> registries) {
        MeterRegistry registry = registries.getIfAvailable();
        if (registry == null) {
            LoggerFactory.getLogger(PlatformSchedulingAutoConfiguration.class)
                    .atWarn()
                    .addKeyValue("errorCode", "PLT-SCHED-004")
                    .log("No MeterRegistry found; distributed scheduling metrics are not exported");
        }
        return new SchedulingLockMetrics(registry);
    }

    private static String requireServiceName(org.springframework.core.env.Environment environment) {
        String serviceName = environment.getProperty("spring.application.name");
        if (serviceName == null || serviceName.isBlank()) {
            throw configurationFailure("spring.application.name is required");
        }
        return serviceName;
    }

    private static void validate(PlatformSchedulingProperties properties) {
        Duration maximum = properties.getLockAtMostFor();
        Duration minimum = properties.getLockAtLeastFor();
        if (maximum == null || maximum.isZero() || maximum.isNegative()
                || minimum == null || minimum.isNegative() || minimum.compareTo(maximum) > 0
                || properties.isKeepAlive() && maximum.compareTo(Duration.ofSeconds(30)) < 0) {
            throw configurationFailure("lock durations are invalid for the selected keep-alive mode");
        }
    }

    private static void verifyRedis(RedisConnectionFactory connectionFactory) {
        try (RedisConnection connection = connectionFactory.getConnection()) {
            connection.ping();
        } catch (RuntimeException failure) {
            throw new PlatformConfigurationException(
                    new PlatformErrorDefinition(
                            "PLT-SCHED-002", "Distributed scheduling lock Redis is unavailable",
                            "Check the Redis connection and retry the service startup.", 500),
                    failure);
        }
    }

    static PlatformConfigurationException configurationFailure(String reason) {
        return new PlatformConfigurationException(new PlatformErrorDefinition(
                "PLT-SCHED-003", "Invalid distributed scheduling lock configuration: " + reason,
                "Review golden.scheduling.distributed-lock and spring.application.name.", 500));
    }
}

final class PlatformSchedulingLockExtensionScheduler {
    private final ScheduledExecutorService executor;

    PlatformSchedulingLockExtensionScheduler(ScheduledExecutorService executor) {
        this.executor = executor;
    }

    ScheduledExecutorService executor() {
        return executor;
    }

    void shutdown() {
        executor.shutdown();
    }
}

@org.springframework.context.annotation.Configuration(proxyBeanMethods = false)
@ConditionalOnProperty(
        prefix = "golden.scheduling.distributed-lock", name = "enabled", havingValue = "true", matchIfMissing = true)
class PlatformSchedulingEnabledConfiguration {
    @Bean
    PlatformSchedulingTaskSchedulerRouter platformSchedulingTaskSchedulerRouter(BeanFactory beanFactory) {
        return new PlatformSchedulingTaskSchedulerRouter(beanFactory);
    }

    @Bean
    @Order(Ordered.LOWEST_PRECEDENCE)
    SchedulingConfigurer platformSchedulingConfigurer(
            LockProvider lockProvider,
            PlatformSchedulingProperties properties,
            org.springframework.core.env.Environment environment,
            PlatformSchedulingTaskSchedulerRouter schedulerRouter,
            SchedulingLockMetrics metrics) {
        String serviceName = environment.getRequiredProperty("spring.application.name");
        return new SchedulingConfigurer() {
            @Override
            public void configureTasks(ScheduledTaskRegistrar registrar) {
                TaskScheduler delegate = registrar.getScheduler();
                if (delegate == null) {
                    delegate = schedulerRouter.router();
                }
                registrar.setTaskScheduler(new LockingTaskScheduler(
                        delegate, lockProvider, serviceName, properties, metrics));
            }
        };
    }
}

/** Owns Spring's fallback router so its internally created executor is shut down with the context. */
final class PlatformSchedulingTaskSchedulerRouter implements org.springframework.beans.factory.DisposableBean {
    private final TaskSchedulerRouter router;

    PlatformSchedulingTaskSchedulerRouter(BeanFactory beanFactory) {
        this.router = new TaskSchedulerRouter();
        this.router.setBeanFactory(beanFactory);
    }

    TaskSchedulerRouter router() {
        return router;
    }

    @Override
    public void destroy() {
        router.destroy();
    }
}
