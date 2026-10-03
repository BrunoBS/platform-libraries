package br.com.portalmanager.platform.library.audit.autoconfigure;

import br.com.portalmanager.platform.library.audit.aspect.AuditAspect;
import br.com.portalmanager.platform.library.audit.config.PlatformAuditProperties;
import br.com.portalmanager.platform.library.audit.context.AuditAuthorizationContextResolver;
import br.com.portalmanager.platform.library.audit.fallback.AuditFallbackStore;
import br.com.portalmanager.platform.library.audit.message.AuditTechnicalErrors;
import br.com.portalmanager.platform.library.audit.publisher.AuditPublisher;
import br.com.portalmanager.platform.library.audit.publisher.RestAuditPublisher;
import br.com.portalmanager.platform.library.messaging.exception.PlatformConfigurationException;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.SmartInitializingSingleton;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.web.client.RestClient;
import tools.jackson.databind.ObjectMapper;

@AutoConfiguration
@EnableConfigurationProperties(PlatformAuditProperties.class)
@ConditionalOnProperty(prefix = "platform.audit", name = "enabled", havingValue = "true", matchIfMissing = true)
public class PlatformAuditAutoConfiguration {

    @Bean
    @ConditionalOnMissingBean(AuditPublisher.class)
    AuditPublisher auditPublisher(
            RestClient.Builder builder,
            PlatformAuditProperties properties,
            ObjectProvider<AuditFallbackStore> fallbackStoreProvider
    ) {
        if (properties.getServiceUrl() == null || properties.getServiceUrl().isBlank()) {
            throw new PlatformConfigurationException(AuditTechnicalErrors.SERVICE_URL_REQUIRED);
        }

        return new RestAuditPublisher(
                builder,
                properties,
                fallbackStoreProvider
        );
    }

    @Bean
    @ConditionalOnProperty(
            prefix = "platform.audit",
            name = {"fail-on-error", "fallback.enabled"},
            havingValue = "false",
            matchIfMissing = false
    )
    SmartInitializingSingleton auditDurableStoreConfigurationGuard(
            ObjectProvider<AuditFallbackStore> fallbackStoreProvider
    ) {
        return () -> {
            if (fallbackStoreProvider.getIfAvailable() == null) {
                throw new PlatformConfigurationException(AuditTechnicalErrors.FALLBACK_STORE_MISSING);
            }
        };
    }

    @Bean
    @ConditionalOnProperty(prefix = "platform.audit.fallback", name = "enabled", havingValue = "true", matchIfMissing = true)
    SmartInitializingSingleton auditFallbackConfigurationGuard(
            ObjectProvider<AuditFallbackStore> fallbackStoreProvider
    ) {
        return () -> {
            if (fallbackStoreProvider.getIfAvailable() == null) {
                throw new PlatformConfigurationException(AuditTechnicalErrors.FALLBACK_STORE_MISSING);
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
