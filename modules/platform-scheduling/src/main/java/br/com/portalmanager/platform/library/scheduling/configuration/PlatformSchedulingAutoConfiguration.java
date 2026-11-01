package br.com.portalmanager.platform.library.scheduling.configuration;

import br.com.portalmanager.platform.library.messaging.exception.PlatformConfigurationException;
import br.com.portalmanager.platform.library.scheduling.internal.BlockingReactiveScheduledMethodPostProcessor;
import br.com.portalmanager.platform.library.scheduling.internal.LockingTaskScheduler;
import br.com.portalmanager.platform.library.scheduling.message.SchedulingTechnicalErrors;
import br.com.portalmanager.platform.library.scheduling.task.PlatformTaskScheduler;
import java.time.Duration;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.atomic.AtomicBoolean;
import net.javacrumbs.shedlock.core.ExtensibleLockProvider;
import net.javacrumbs.shedlock.core.LockProvider;
import net.javacrumbs.shedlock.provider.redis.spring.RedisLockProvider;
import net.javacrumbs.shedlock.support.KeepAliveLockProvider;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.BeanFactory;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.SmartInitializingSingleton;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.context.SmartLifecycle;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.core.env.Environment;
import org.springframework.data.redis.connection.RedisConnection;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.scheduling.TaskScheduler;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.SchedulingConfigurer;
import org.springframework.scheduling.config.ScheduledTaskRegistrar;
import org.springframework.scheduling.config.TaskSchedulerRouter;

@AutoConfiguration
@EnableScheduling
@EnableConfigurationProperties(PlatformSchedulingProperties.class)
@Import(PlatformSchedulingEnabledConfiguration.class)
public class PlatformSchedulingAutoConfiguration {
    private static final String REDIS_LOCK_KEY_CATEGORY = "job-lock";
    private static final String LOCK_KEY_CATEGORY = "job-lock";
    @Bean
    @ConditionalOnProperty(
            prefix = "golden.scheduling.distributed-lock", name = "enabled", havingValue = "false")
    SmartInitializingSingleton platformSchedulingBypassWarning() {
        return () -> LoggerFactory.getLogger(PlatformSchedulingAutoConfiguration.class)
                .atWarn()
                .addKeyValue("errorCode", SchedulingTechnicalErrors.LOCK_DISABLED.code())
                .log("Distributed scheduled lock disabled; scheduled tasks run without coordination");
    }

    @Bean
    @ConditionalOnProperty(
            prefix = "golden.scheduling.distributed-lock", name = "enabled", havingValue = "true", matchIfMissing = true)
    LockProvider platformSchedulingLockProvider(
            RedisConnectionFactory connectionFactory,
            PlatformSchedulingProperties properties,
            Environment environment,
            ObjectProvider<PlatformSchedulingLockExtensionScheduler> extensionScheduler) {
        String serviceName = requireServiceName(environment);
        validate(properties);
        verifyRedis(connectionFactory);
        ExtensibleLockProvider provider = new RedisLockProvider(connectionFactory, LOCK_KEY_CATEGORY, serviceName);
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
                    SchedulingTechnicalErrors.REDIS_UNAVAILABLE,
                    failure);
        }
    }

    static PlatformConfigurationException configurationFailure(String reason) {
        return new PlatformConfigurationException(SchedulingTechnicalErrors.invalidConfiguration(reason));
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

@Configuration(proxyBeanMethods = false)
@ConditionalOnProperty(
        prefix = "golden.scheduling.distributed-lock", name = "enabled", havingValue = "true", matchIfMissing = true)
class PlatformSchedulingEnabledConfiguration {
    @Bean
    static BlockingReactiveScheduledMethodPostProcessor platformBlockingReactiveScheduledMethodPostProcessor() {
        return new BlockingReactiveScheduledMethodPostProcessor();
    }

    @Bean
    PlatformTaskScheduler platformTaskScheduler(
            LockProvider lockProvider,
            PlatformSchedulingProperties properties,
            Environment environment,
            PlatformSchedulingTaskSchedulerRouter schedulerRouter) {
        LockingTaskScheduler lockingScheduler = new LockingTaskScheduler(
                schedulerRouter.router(), lockProvider,
                environment.getRequiredProperty("spring.application.name"), properties);
        schedulerRouter.setLockingScheduler(lockingScheduler);
        return new PlatformTaskScheduler(() -> lockingScheduler);
    }

    @Bean
    PlatformSchedulingTaskSchedulerRouter platformSchedulingTaskSchedulerRouter(BeanFactory beanFactory) {
        return new PlatformSchedulingTaskSchedulerRouter(beanFactory);
    }

    @Bean
    @Order(Ordered.LOWEST_PRECEDENCE)
    SchedulingConfigurer platformSchedulingConfigurer(
            LockProvider lockProvider,
            PlatformSchedulingProperties properties,
            Environment environment,
            PlatformSchedulingTaskSchedulerRouter schedulerRouter) {
        String serviceName = environment.getRequiredProperty("spring.application.name");
        return registrar -> {
            TaskScheduler delegate = registrar.getScheduler();
            if (delegate == null) {
                delegate = schedulerRouter.router();
            }
            LockingTaskScheduler lockingScheduler = schedulerRouter.lockingScheduler();
            if (lockingScheduler == null) {
                lockingScheduler = new LockingTaskScheduler(
                        delegate, lockProvider, serviceName, properties);
            }
            schedulerRouter.setLockingScheduler(lockingScheduler);
            registrar.setTaskScheduler(lockingScheduler);
        };
    }
}

/** Owns Spring's fallback router so its internally created executor is shut down with the context. */
final class PlatformSchedulingTaskSchedulerRouter
        implements SmartLifecycle, DisposableBean {
    private final TaskSchedulerRouter router;
    private volatile LockingTaskScheduler lockingScheduler;
    private final AtomicBoolean stopped = new AtomicBoolean();
    private volatile boolean running = true;

    PlatformSchedulingTaskSchedulerRouter(BeanFactory beanFactory) {
        this.router = new TaskSchedulerRouter();
        this.router.setBeanFactory(beanFactory);
    }

    TaskSchedulerRouter router() {
        return router;
    }

    @Override
    public void start() {
        running = true;
    }

    @Override
    public void stop() {
        stopScheduler();
    }

    @Override
    public void stop(Runnable callback) {
        stopScheduler();
        callback.run();
    }

    @Override
    public boolean isRunning() {
        return running;
    }

    @Override
    public boolean isAutoStartup() {
        return true;
    }

    @Override
    public int getPhase() {
        return Integer.MAX_VALUE;
    }

    void setLockingScheduler(LockingTaskScheduler lockingScheduler) {
        this.lockingScheduler = lockingScheduler;
    }

    LockingTaskScheduler lockingScheduler() {
        return lockingScheduler;
    }

    @Override
    public void destroy() {
        stopScheduler();
    }

    private void stopScheduler() {
        if (!stopped.compareAndSet(false, true)) {
            return;
        }
        router.destroy();
        LockingTaskScheduler lockingScheduler = this.lockingScheduler;
        if (lockingScheduler != null) {
            lockingScheduler.shutdownAndAwaitInFlightTasks();
        }
        running = false;
    }
}
