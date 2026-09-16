package com.empresa.platform.audit.autoconfigure;

import com.empresa.platform.audit.client.AuditEventClient;
import com.empresa.platform.audit.config.PlatformAuditProperties;
import com.empresa.platform.audit.fallback.AuditFallbackStore;
import com.empresa.platform.audit.fallback.RedisAuditFallbackStore;
import com.empresa.platform.audit.recovery.AuditRecoveryScheduler;
import com.empresa.platform.audit.recovery.AuditRecoveryWorker;
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
            StringRedisTemplate redisTemplate,
            ObjectMapper objectMapper,
            PlatformAuditProperties properties
    ) {
        return new RedisAuditFallbackStore(redisTemplate, objectMapper, properties);
    }

    @Bean
    @ConditionalOnMissingBean(AuditRecoveryWorker.class)
    AuditRecoveryWorker auditRecoveryWorker(
            AuditEventClient client,
            AuditFallbackStore fallbackStore,
            PlatformAuditProperties properties
    ) {
        return new AuditRecoveryWorker(client, fallbackStore, properties);
    }

    @Bean(name = "platformAuditRecoveryTaskScheduler")
    @ConditionalOnMissingBean(name = "platformAuditRecoveryTaskScheduler")
    ThreadPoolTaskScheduler platformAuditRecoveryTaskScheduler() {
        ThreadPoolTaskScheduler scheduler = new ThreadPoolTaskScheduler();
        scheduler.setPoolSize(1);
        scheduler.setThreadNamePrefix("platform-audit-recovery-");
        return scheduler;
    }

    @Bean
    @ConditionalOnMissingBean(AuditRecoveryScheduler.class)
    AuditRecoveryScheduler auditRecoveryScheduler(
            AuditRecoveryWorker worker,
            ThreadPoolTaskScheduler platformAuditRecoveryTaskScheduler,
            PlatformAuditProperties properties
    ) {
        return new AuditRecoveryScheduler(
                worker,
                platformAuditRecoveryTaskScheduler,
                properties.getFallback().getRecoveryInterval()
        );
    }
}
