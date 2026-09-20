package com.empresa.platform.audit.autoconfigure;

import com.empresa.platform.audit.aspect.AuditAspect;
import com.empresa.platform.audit.config.PlatformAuditProperties;
import com.empresa.platform.audit.context.AuditAuthorizationContextResolver;
import com.empresa.platform.audit.exception.AuditException;
import com.empresa.platform.audit.fallback.AuditFallbackStore;
import com.empresa.platform.audit.message.AuditMessageKeys;
import com.empresa.platform.audit.publisher.AuditPublisher;
import com.empresa.platform.audit.publisher.RestAuditPublisher;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.SmartInitializingSingleton;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.core.task.TaskExecutor;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.web.client.RestClient;
import tools.jackson.databind.ObjectMapper;

@AutoConfiguration
@EnableConfigurationProperties(PlatformAuditProperties.class)
@ConditionalOnProperty(prefix = "platform.audit", name = "enabled", havingValue = "true", matchIfMissing = true)
public class PlatformAuditAutoConfiguration {

    @Bean(name = "platformAuditTaskExecutor")
    @ConditionalOnMissingBean(name = "platformAuditTaskExecutor")
    TaskExecutor platformAuditTaskExecutor(PlatformAuditProperties properties) {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setThreadNamePrefix("platform-audit-");
        executor.setCorePoolSize(properties.getCorePoolSize());
        executor.setMaxPoolSize(properties.getMaxPoolSize());
        executor.setQueueCapacity(properties.getQueueCapacity());
        executor.initialize();
        return executor;
    }

    @Bean
    @ConditionalOnMissingBean(AuditPublisher.class)
    AuditPublisher auditPublisher(
            RestClient.Builder builder,
            @Qualifier("platformAuditTaskExecutor") TaskExecutor platformAuditTaskExecutor,
            PlatformAuditProperties properties,
            ObjectProvider<AuditFallbackStore> fallbackStoreProvider
    ) {
        if (properties.getServiceUrl() == null || properties.getServiceUrl().isBlank()) {
            throw new AuditException(AuditMessageKeys.SERVICE_URL_REQUIRED);
        }

        return new RestAuditPublisher(
                builder,
                platformAuditTaskExecutor,
                properties,
                fallbackStoreProvider
        );
    }

    @Bean
    @ConditionalOnProperty(prefix = "platform.audit.fallback", name = "enabled", havingValue = "true")
    SmartInitializingSingleton auditFallbackConfigurationGuard(
            ObjectProvider<AuditFallbackStore> fallbackStoreProvider
    ) {
        return () -> {
            if (fallbackStoreProvider.getIfAvailable() == null) {
                throw new AuditException(AuditMessageKeys.FALLBACK_STORE_MISSING);
            }
        };
    }

    @Bean
    @ConditionalOnMissingBean(AuditAuthorizationContextResolver.class)
    AuditAuthorizationContextResolver auditAuthorizationContextResolver() {
        return new AuditAuthorizationContextResolver();
    }

    @Bean
    @ConditionalOnMissingBean(AuditAspect.class)
    AuditAspect auditAspect(
            PlatformAuditProperties properties,
            AuditPublisher publisher,
            AuditAuthorizationContextResolver contextResolver,
            ObjectMapper objectMapper,
            HttpServletRequest request
    ) {
        return new AuditAspect(properties, publisher, contextResolver, objectMapper, request);
    }
}
