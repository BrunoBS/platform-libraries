package br.com.portalmanager.platform.library.authorization.config;

import br.com.portalmanager.platform.library.authorization.model.AuthorizationContextPropagation;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.core.task.TaskDecorator;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

/**
 * Opt-in context propagation for @Async("authorizationExecutor").
 * Does not replace or mutate application-defined executors.
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
    public ThreadPoolTaskExecutor authorizationExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setThreadNamePrefix("authorization-async-");
        executor.setCorePoolSize(4);
        executor.setMaxPoolSize(16);
        executor.setQueueCapacity(100);
        executor.setTaskDecorator(AuthorizationContextPropagation.taskDecorator());
        executor.initialize();
        return executor;
    }
}
