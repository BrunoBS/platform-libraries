package br.com.portalmanager.platform.library.audit.autoconfigure;

import br.com.portalmanager.platform.library.audit.aspect.AuditAspect;
import br.com.portalmanager.platform.library.audit.aspect.AuditInvocationEventFactory;
import br.com.portalmanager.platform.library.audit.aspect.AuditSnapshotCollector;
import br.com.portalmanager.platform.library.audit.config.AuditPropertiesValidator;
import br.com.portalmanager.platform.library.audit.config.PlatformAuditProperties;
import br.com.portalmanager.platform.library.audit.context.AuditAuthorizationContextResolver;
import br.com.portalmanager.platform.library.audit.event.AuditEventFactory;
import br.com.portalmanager.platform.library.audit.outbox.AuditBeforeSnapshotProvider;
import br.com.portalmanager.platform.library.audit.outbox.AuditOutboxStore;
import br.com.portalmanager.platform.library.audit.outbox.JpaAuditOutboxStore;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import jakarta.persistence.EntityManager;
import org.springframework.transaction.annotation.EnableTransactionManagement;
import tools.jackson.databind.ObjectMapper;

@AutoConfiguration
@EnableTransactionManagement(order = 100)
@EnableConfigurationProperties(PlatformAuditProperties.class)
@ConditionalOnProperty(prefix = "platform.audit", name = "enabled", havingValue = "true", matchIfMissing = true)
public class PlatformAuditAutoConfiguration {

    @Bean
    AuditPropertiesValidator auditPropertiesValidator(PlatformAuditProperties properties) {
        return new AuditPropertiesValidator(properties);
    }

    @Bean
    @ConditionalOnMissingBean(AuditAuthorizationContextResolver.class)
    AuditAuthorizationContextResolver auditAuthorizationContextResolver() {
        return new AuditAuthorizationContextResolver();
    }

    @Bean
    @ConditionalOnMissingBean(AuditEventFactory.class)
    AuditEventFactory auditEventFactory(
            PlatformAuditProperties properties,
            AuditAuthorizationContextResolver contextResolver,
            ObjectMapper objectMapper
    ) {
        return new AuditEventFactory(properties, contextResolver, objectMapper);
    }

    @Bean
    @ConditionalOnMissingBean(AuditSnapshotCollector.class)
    AuditSnapshotCollector auditSnapshotCollector(
            org.springframework.beans.factory.ObjectProvider<AuditBeforeSnapshotProvider> beforeSnapshotProviders
    ) {
        return new AuditSnapshotCollector(beforeSnapshotProviders.getIfAvailable());
    }

    @Bean
    @ConditionalOnMissingBean(AuditOutboxStore.class)
    AuditOutboxStore auditOutboxStore(EntityManager entityManager) {
        return new JpaAuditOutboxStore(entityManager);
    }

    @Bean
    @ConditionalOnMissingBean(AuditInvocationEventFactory.class)
    AuditInvocationEventFactory auditInvocationEventFactory(AuditEventFactory eventFactory) {
        return new AuditInvocationEventFactory(eventFactory);
    }

    @Bean
    @ConditionalOnMissingBean(AuditAspect.class)
    AuditAspect auditAspect(
            AuditSnapshotCollector snapshotCollector,
            AuditInvocationEventFactory eventFactory,
            AuditOutboxStore outboxStore
    ) {
        return new AuditAspect(snapshotCollector, eventFactory, outboxStore);
    }
}
