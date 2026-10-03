package br.com.portalmanager.platform.library.audit.autoconfigure;

import br.com.portalmanager.platform.library.audit.queue.AuditEventQueue;
import br.com.portalmanager.platform.library.audit.publisher.AuditPublisher;
import br.com.portalmanager.platform.library.audit.recovery.AuditRecoveryLock;
import br.com.portalmanager.platform.library.audit.recovery.AuditRecoveryService;
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
        AuditPublisher auditPublisher() {
            return mock(AuditPublisher.class);
        }

        @Bean
        ObjectMapper objectMapper() {
            return new ObjectMapper();
        }
    }

    @Test
    void shouldCreateRedisQueueBeansWhenEnabled() {
        contextRunner
                .withPropertyValues(
                        "platform.audit.enabled=true",
                        "platform.audit.service-name=account",
                        "platform.audit.queue.enabled=true",
                        "platform.audit.queue.recovery-interval=PT5M",
                        "platform.audit.queue.batch-size=25"
                )
                .run(context -> {
                    assertThat(context).hasSingleBean(AuditEventQueue.class);
                    assertThat(context).hasSingleBean(AuditRecoveryLock.class);
                    assertThat(context).hasSingleBean(AuditRecoveryService.class);
                    assertThat(context).hasBean("platformAuditRecoveryTaskScheduler");
                });
    }

    @Test
    void shouldNotCreateRedisQueueBeansWhenDisabled() {
        contextRunner
                .withPropertyValues(
                        "platform.audit.enabled=true",
                        "platform.audit.queue.enabled=false"
                )
                .run(context -> {
                    assertThat(context).doesNotHaveBean(AuditEventQueue.class);
                    assertThat(context).doesNotHaveBean(AuditRecoveryLock.class);
                    assertThat(context).doesNotHaveBean(AuditRecoveryService.class);
                    assertThat(context).doesNotHaveBean("platformAuditRecoveryTaskScheduler");
                });
    }
}
