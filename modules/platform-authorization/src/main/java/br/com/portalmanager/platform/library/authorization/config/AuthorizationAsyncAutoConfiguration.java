package br.com.portalmanager.platform.library.authorization.config;

import br.com.portalmanager.platform.library.authorization.model.AuthorizationContextPropagation;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.core.task.TaskDecorator;
import org.springframework.scheduling.annotation.AsyncConfigurer;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import java.util.concurrent.Executor;

@AutoConfiguration(after = PlatformAuthorizationAutoConfiguration.class)
@EnableConfigurationProperties(PlatformAuthorizationProperties.class)
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
    public ThreadPoolTaskExecutor authorizationExecutor(PlatformAuthorizationProperties properties) {
        var settings = properties.getContextPropagation();
        if (settings.getCoreSize() < 1 || settings.getMaxSize() < settings.getCoreSize()
                || settings.getQueueCapacity() < 0 || settings.getKeepAlive() == null
                || settings.getKeepAlive().isNegative() || settings.getThreadNamePrefix() == null
                || settings.getThreadNamePrefix().isBlank()) {
            throw new IllegalArgumentException("Invalid platform.authorization.context-propagation executor settings");
        }
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setThreadNamePrefix(settings.getThreadNamePrefix());
        executor.setCorePoolSize(settings.getCoreSize());
        executor.setMaxPoolSize(settings.getMaxSize());
        executor.setQueueCapacity(settings.getQueueCapacity());
        executor.setKeepAliveSeconds(Math.toIntExact(settings.getKeepAlive().toSeconds()));
        executor.setTaskDecorator(AuthorizationContextPropagation.taskDecorator());
        return executor;
    }

    @Bean
    @ConditionalOnProperty(prefix = "platform.authorization.context-propagation",
            name = "default-executor", havingValue = "true")
    @ConditionalOnMissingBean(AsyncConfigurer.class)
    public AsyncConfigurer authorizationAsyncConfigurer(
            @org.springframework.beans.factory.annotation.Qualifier("authorizationExecutor") Executor executor) {
        return new AsyncConfigurer() {
            @Override
            public Executor getAsyncExecutor() {
                return executor;
            }
        };
    }
}
