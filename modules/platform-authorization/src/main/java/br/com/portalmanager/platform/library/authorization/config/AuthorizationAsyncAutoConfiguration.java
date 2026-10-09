package br.com.portalmanager.platform.library.authorization.config;

import br.com.portalmanager.platform.library.authorization.model.AuthorizationContextPropagation;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.core.env.Environment;
import org.springframework.core.task.TaskDecorator;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

/**
 * Opt-in context propagation for @Async("authorizationExecutor").
 * Respects application-provided executors and Spring task execution pool settings.
 */
@AutoConfiguration(after = PlatformAuthorizationAutoConfiguration.class)
@ConditionalOnProperty(prefix = "platform.authorization.context-propagation",
        name = "enabled", havingValue = "true")
public class AuthorizationAsyncAutoConfiguration {

    @Bean("authorizationContextTaskDecorator")
    @ConditionalOnMissingBean(name = "authorizationContextTaskDecorator")
    public TaskDecorator authorizationContextTaskDecorator() {
        return AuthorizationContextPropagation.taskDecorator();
    }

    @Bean("authorizationExecutor")
    @ConditionalOnMissingBean(name = "authorizationExecutor")
    public ThreadPoolTaskExecutor authorizationExecutor(Environment environment) {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setThreadNamePrefix(environment.getProperty(
                "spring.task.execution.thread-name-prefix", "authorization-async-"));
        Integer coreSize = environment.getProperty("spring.task.execution.pool.core-size", Integer.class);
        Integer maxSize = environment.getProperty("spring.task.execution.pool.max-size", Integer.class);
        Integer queueCapacity = environment.getProperty("spring.task.execution.pool.queue-capacity", Integer.class);
        Integer keepAlive = environment.getProperty("spring.task.execution.pool.keep-alive", java.time.Duration.class) == null
                ? null
                : Math.toIntExact(environment.getRequiredProperty(
                        "spring.task.execution.pool.keep-alive", java.time.Duration.class).toSeconds());
        if (coreSize != null) {
            executor.setCorePoolSize(coreSize);
        }
        if (maxSize != null) {
            executor.setMaxPoolSize(maxSize);
        }
        if (queueCapacity != null) {
            executor.setQueueCapacity(queueCapacity);
        }
        if (keepAlive != null) {
            executor.setKeepAliveSeconds(keepAlive);
        }
        executor.setTaskDecorator(AuthorizationContextPropagation.taskDecorator());
        executor.initialize();
        return executor;
    }
}
