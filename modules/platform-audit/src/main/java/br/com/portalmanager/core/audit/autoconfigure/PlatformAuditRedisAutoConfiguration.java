package br.com.portalmanager.core.audit.autoconfigure;

import br.com.portalmanager.core.audit.config.PlatformAuditProperties;
import br.com.portalmanager.core.audit.exception.AuditException;
import br.com.portalmanager.core.audit.fallback.AuditFallbackStore;
import br.com.portalmanager.core.audit.fallback.RedisAuditFallbackStore;
import br.com.portalmanager.core.audit.message.AuditMessageKeys;
import br.com.portalmanager.core.audit.publisher.AuditPublisher;
import br.com.portalmanager.core.audit.recovery.AuditRecoveryLock;
import br.com.portalmanager.core.audit.recovery.AuditRecoveryService;
import br.com.portalmanager.core.audit.recovery.RedisAuditRecoveryLock;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler;
import tools.jackson.databind.ObjectMapper;

@AutoConfiguration
@EnableConfigurationProperties(PlatformAuditProperties.class)
@ConditionalOnClass(StringRedisTemplate.class)
@ConditionalOnProperty(prefix = "platform.audit", name = "enabled", havingValue = "true", matchIfMissing = true)
@ConditionalOnProperty(prefix = "platform.audit.fallback", name = "enabled", havingValue = "true")
public class PlatformAuditRedisAutoConfiguration {

    @Bean
    @ConditionalOnMissingBean(AuditFallbackStore.class)
    AuditFallbackStore auditFallbackStore(
            ObjectProvider<StringRedisTemplate> redisTemplateProvider,
            ObjectMapper objectMapper,
            PlatformAuditProperties properties
    ) {
        StringRedisTemplate redisTemplate = redisTemplateProvider.getIfAvailable();
        if (redisTemplate == null) {
            throw new AuditException(AuditMessageKeys.REDIS_NOT_CONFIGURED);
        }
        return new RedisAuditFallbackStore(redisTemplate, objectMapper, properties);
    }

    @Bean
    @ConditionalOnMissingBean(AuditRecoveryLock.class)
    AuditRecoveryLock auditRecoveryLock(
            ObjectProvider<StringRedisTemplate> redisTemplateProvider,
            PlatformAuditProperties properties
    ) {
        StringRedisTemplate redisTemplate = redisTemplateProvider.getIfAvailable();
        if (redisTemplate == null) {
            throw new AuditException(AuditMessageKeys.REDIS_NOT_CONFIGURED);
        }
        return new RedisAuditRecoveryLock(redisTemplate, properties);
    }

    @Bean(name = "platformAuditRecoveryTaskScheduler")
    @ConditionalOnMissingBean(name = "platformAuditRecoveryTaskScheduler")
    ThreadPoolTaskScheduler platformAuditRecoveryTaskScheduler() {
        ThreadPoolTaskScheduler scheduler = new ThreadPoolTaskScheduler();
        scheduler.setPoolSize(1);
        scheduler.setThreadNamePrefix("platform-audit-recovery-");
        scheduler.initialize();
        return scheduler;
    }

    @Bean
    @ConditionalOnMissingBean(AuditRecoveryService.class)
    AuditRecoveryService auditRecoveryService(
            AuditPublisher publisher,
            AuditFallbackStore fallbackStore,
            AuditRecoveryLock recoveryLock,
            @Qualifier("platformAuditRecoveryTaskScheduler")
            ThreadPoolTaskScheduler platformAuditRecoveryTaskScheduler,
            PlatformAuditProperties properties
    ) {
        return new AuditRecoveryService(
                publisher,
                fallbackStore,
                recoveryLock,
                platformAuditRecoveryTaskScheduler,
                properties
        );
    }
}
