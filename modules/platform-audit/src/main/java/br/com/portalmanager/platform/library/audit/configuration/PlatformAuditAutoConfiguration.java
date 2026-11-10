package br.com.portalmanager.platform.library.audit.configuration;

import br.com.portalmanager.platform.library.audit.internal.aspect.AuditAspect;
import br.com.portalmanager.platform.library.audit.internal.aspect.AuditInvocationEventFactory;
import br.com.portalmanager.platform.library.audit.internal.aspect.AuditSnapshotCollector;
import br.com.portalmanager.platform.library.audit.api.configuration.PlatformAuditProperties;
import br.com.portalmanager.platform.library.audit.internal.context.AuditAuthorizationContextResolver;
import br.com.portalmanager.platform.library.audit.internal.event.AuditEventFactory;
import br.com.portalmanager.platform.library.audit.internal.outbox.AuditOutboxAppender;
import br.com.portalmanager.platform.library.audit.internal.outbox.AuditOutboxAfterCommitListener;
import br.com.portalmanager.platform.library.audit.internal.outbox.AuditOutboxProcessor;
import br.com.portalmanager.platform.library.audit.api.contract.outbox.AuditOutboxRepository;
import br.com.portalmanager.platform.library.audit.internal.outbox.AuditOutboxSchedulers;
import br.com.portalmanager.platform.library.objectstorage.api.configuration.ObjectStorageAutoConfiguration;
import br.com.portalmanager.platform.library.objectstorage.api.configuration.ObjectStorageProperties;
import br.com.portalmanager.platform.library.objectstorage.api.contract.ObjectStorageClient;
import br.com.portalmanager.platform.library.scheduling.task.PlatformTaskScheduler;
import jakarta.persistence.EntityManager;
import org.springframework.core.task.TaskExecutor;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.beans.factory.ListableBeanFactory;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.data.repository.support.Repositories;
import org.springframework.transaction.annotation.EnableTransactionManagement;
import tools.jackson.databind.ObjectMapper;

@AutoConfiguration(after = ObjectStorageAutoConfiguration.class)
@EnableTransactionManagement(order = 100)
@EnableConfigurationProperties(PlatformAuditProperties.class)
@ConditionalOnProperty(prefix = "platform.audit", name = "enabled", havingValue = "true", matchIfMissing = true)
public class PlatformAuditAutoConfiguration {

    @Bean
    AuditPropertiesValidator auditPropertiesValidator(
            PlatformAuditProperties properties,
            ObjectProvider<ObjectStorageProperties> objectStorageProperties,
            ObjectProvider<ObjectStorageClient> objectStorageClient
    ) {
        return new AuditPropertiesValidator(properties, objectStorageProperties, objectStorageClient);
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
    AuditSnapshotCollector auditSnapshotCollector() {
        return new AuditSnapshotCollector();
    }

    @Bean
    @ConditionalOnMissingBean(AuditOutboxAppender.class)
    AuditOutboxAppender auditOutboxAppender(
            AuditOutboxRepository<?> repository,
            EntityManager entityManager,
            ListableBeanFactory beanFactory,
            org.springframework.context.ApplicationEventPublisher eventPublisher
    ) {
        return new AuditOutboxAppender(repository, entityManager, new Repositories(beanFactory), eventPublisher);
    }

    @Bean
    @ConditionalOnMissingBean(name = "auditOutboxTaskExecutor")
    TaskExecutor auditOutboxTaskExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(2);
        executor.setMaxPoolSize(4);
        executor.setQueueCapacity(500);
        executor.setThreadNamePrefix("platform-audit-outbox-");
        return executor;
    }

    @Bean
    @ConditionalOnMissingBean
    AuditOutboxProcessor auditOutboxProcessor(
            AuditOutboxRepository<?> repository,
            ObjectStorageClient objectStorageClient,
            PlatformAuditProperties properties,
            ObjectMapper objectMapper,
            PlatformTransactionManager transactionManager
    ) {
        return new AuditOutboxProcessor(repository, objectStorageClient, properties, objectMapper, transactionManager);
    }

    @Bean
    @ConditionalOnMissingBean
    AuditOutboxAfterCommitListener auditOutboxAfterCommitListener(
            @Qualifier("auditOutboxTaskExecutor") TaskExecutor auditOutboxTaskExecutor,
            AuditOutboxProcessor processor
    ) {
        return new AuditOutboxAfterCommitListener(auditOutboxTaskExecutor, processor);
    }

    @Bean(initMethod = "start", destroyMethod = "close")
    @ConditionalOnMissingBean
    AuditOutboxSchedulers auditOutboxSchedulers(PlatformTaskScheduler scheduler, AuditOutboxProcessor processor) {
        return new AuditOutboxSchedulers(scheduler, processor);
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
            AuditOutboxAppender outboxAppender
    ) {
        return new AuditAspect(snapshotCollector, eventFactory, outboxAppender);
    }
}
