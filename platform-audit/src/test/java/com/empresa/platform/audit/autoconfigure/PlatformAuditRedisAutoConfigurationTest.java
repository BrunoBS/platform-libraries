package com.empresa.platform.audit.autoconfigure;

import com.empresa.platform.audit.client.AuditEventClient;
import com.empresa.platform.audit.fallback.AuditFallbackStore;
import com.empresa.platform.audit.recovery.AuditRecoveryScheduler;
import com.empresa.platform.audit.recovery.AuditRecoveryWorker;
import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.core.StringRedisTemplate;
import tools.jackson.databind.ObjectMapper;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

class PlatformAuditRedisAutoConfigurationTest {

    private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
            .withConfiguration(AutoConfigurations.of(PlatformAuditRedisAutoConfiguration.class))
            .withUserConfiguration(RedisInfrastructure.class);

    @Configuration
    static class RedisInfrastructure {

        @Bean
        StringRedisTemplate stringRedisTemplate() {
            return mock(StringRedisTemplate.class);
        }

        @Bean
        AuditEventClient auditEventClient() {
            return mock(AuditEventClient.class);
        }

        @Bean
        ObjectMapper objectMapper() {
            return new ObjectMapper();
        }
    }

    @Test
    void shouldCreateRedisFallbackBeansWhenEnabled() {
        contextRunner
                .withPropertyValues(
                        "platform.audit.enabled=true",
                        "platform.audit.service-name=account",
                        "platform.audit.fallback.enabled=true",
                        "platform.audit.fallback.recovery-interval=PT5M",
                        "platform.audit.fallback.batch-size=25"
                )
                .run(context -> {
                    assertThat(context).hasSingleBean(AuditFallbackStore.class);
                    assertThat(context).hasSingleBean(AuditRecoveryWorker.class);
                    assertThat(context).hasSingleBean(AuditRecoveryScheduler.class);
                    assertThat(context).hasBean("platformAuditRecoveryTaskScheduler");
                });
    }

    @Test
    void shouldNotCreateRedisFallbackBeansWhenDisabled() {
        contextRunner
                .withPropertyValues(
                        "platform.audit.enabled=true",
                        "platform.audit.fallback.enabled=false"
                )
                .run(context -> {
                    assertThat(context).doesNotHaveBean(AuditFallbackStore.class);
                    assertThat(context).doesNotHaveBean(AuditRecoveryWorker.class);
                    assertThat(context).doesNotHaveBean(AuditRecoveryScheduler.class);
                    assertThat(context).doesNotHaveBean("platformAuditRecoveryTaskScheduler");
                });
    }
}
