package com.empresa.platform.audit.autoconfigure;

import com.empresa.platform.audit.aspect.AuditAspect;
import com.empresa.platform.audit.client.AuditEventClient;
import com.empresa.platform.audit.client.RestAuditEventClient;
import com.empresa.platform.audit.config.AuditFallbackConfigurationValidator;
import com.empresa.platform.audit.config.PlatformAuditProperties;
import com.empresa.platform.audit.context.AuditContextProvider;
import com.empresa.platform.audit.context.HttpAuditContextProvider;
import com.empresa.platform.audit.fallback.AuditFallbackStore;
import com.empresa.platform.audit.publisher.AuditPublisher;
import com.empresa.platform.audit.publisher.DefaultAuditPublisher;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.core.task.TaskExecutor;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.web.client.RestClient;
import tools.jackson.databind.ObjectMapper;

@AutoConfiguration
@EnableConfigurationProperties(PlatformAuditProperties.class)
@ConditionalOnProperty(prefix = "platform.audit", name = "enabled", havingValue = "true", matchIfMissing = true)
public class PlatformAuditAutoConfiguration {

    @Bean
    @ConditionalOnMissingBean(AuditEventClient.class)
    AuditEventClient auditEventClient(
            RestClient.Builder builder,
            PlatformAuditProperties properties
    ) {
        if (properties.getServiceUrl() == null || properties.getServiceUrl().isBlank()) {
            throw new IllegalArgumentException(
                    "A propriedade [platform.audit.service-url] e obrigatoria quando o modulo de auditoria esta ativo."
            );
        }
        return new RestAuditEventClient(builder, properties);
    }

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
            AuditEventClient client,
            @Qualifier("platformAuditTaskExecutor") TaskExecutor platformAuditTaskExecutor,
            PlatformAuditProperties properties,
            ObjectProvider<AuditFallbackStore> fallbackStoreProvider
    ) {
        return new DefaultAuditPublisher(
                client,
                platformAuditTaskExecutor,
                properties,
                fallbackStoreProvider
        );
    }

    @Bean
    @ConditionalOnProperty(prefix = "platform.audit.fallback", name = "enabled", havingValue = "true")
    AuditFallbackConfigurationValidator auditFallbackConfigurationValidator(
            ObjectProvider<AuditFallbackStore> fallbackStoreProvider
    ) {
        return new AuditFallbackConfigurationValidator(fallbackStoreProvider);
    }

    @Bean
    @ConditionalOnMissingBean(AuditContextProvider.class)
    AuditContextProvider auditContextProvider(HttpServletRequest request) {
        return new HttpAuditContextProvider(request);
    }

    @Bean
    @ConditionalOnMissingBean(AuditAspect.class)
    AuditAspect auditAspect(
            PlatformAuditProperties properties,
            AuditPublisher publisher,
            AuditContextProvider contextProvider,
            ObjectMapper objectMapper,
            HttpServletRequest request
    ) {
        return new AuditAspect(properties, publisher, contextProvider, objectMapper, request);
    }
}
