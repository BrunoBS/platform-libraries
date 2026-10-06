package br.com.portalmanager.platform.library.audit.outbox;

import br.com.portalmanager.platform.library.audit.annotation.Auditable;
import br.com.portalmanager.platform.library.audit.context.AuditAuthorizationContextResolver;
import br.com.portalmanager.platform.library.audit.model.AuditAction;
import br.com.portalmanager.platform.library.audit.model.AuditContext;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringBootConfiguration;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.annotation.Transactional;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@SpringBootTest(
        classes = AuditOutboxTransactionIT.TestApplication.class,
        properties = {
                "spring.application.name=platform-audit-transaction-test",
                "platform.audit.enabled=true",
                "platform.audit.service-name=transaction-test",
                "spring.datasource.url=jdbc:h2:mem:audit_outbox;DB_CLOSE_DELAY=-1;MODE=MySQL",
                "spring.datasource.username=sa",
                "spring.datasource.password=",
                "spring.jpa.hibernate.ddl-auto=create-drop",
                "spring.flyway.enabled=false"
        }
)
class AuditOutboxTransactionIT {

    @Autowired JdbcTemplate jdbcTemplate;
    @Autowired AuditedUseCase useCase;

    @BeforeEach
    void prepareDomainTable() {
        jdbcTemplate.execute("ALTER TABLE AUDIT_OUTBOX DROP CONSTRAINT IF EXISTS CK_AUDIT_OUTBOX_TEST");
        jdbcTemplate.execute("CREATE TABLE IF NOT EXISTS DOMAIN_CHANGE (IDENTIFIER VARCHAR(36) PRIMARY KEY)");
        jdbcTemplate.update("DELETE FROM DOMAIN_CHANGE");
        jdbcTemplate.update("DELETE FROM AUDIT_OUTBOX");
    }

    @Test
    void shouldCommitDomainAndOutboxRowsTogether() {
        useCase.execute(false);

        assertThat(jdbcTemplate.queryForObject("SELECT COUNT(*) FROM DOMAIN_CHANGE", Integer.class)).isEqualTo(1);
        assertThat(jdbcTemplate.queryForObject("SELECT COUNT(*) FROM AUDIT_OUTBOX", Integer.class)).isEqualTo(1);
        String payload = jdbcTemplate.queryForObject("SELECT payload FROM AUDIT_OUTBOX", String.class);
        assertThat(payload).contains("\"identifier\":\"resource-1\"");
    }

    @Test
    void shouldRollbackDomainChangeWhenUseCaseFailsBeforeOutboxCapture() {
        assertThatThrownBy(() -> useCase.execute(true)).isInstanceOf(IllegalStateException.class);

        assertThat(jdbcTemplate.queryForObject("SELECT COUNT(*) FROM DOMAIN_CHANGE", Integer.class)).isZero();
        assertThat(jdbcTemplate.queryForObject("SELECT COUNT(*) FROM AUDIT_OUTBOX", Integer.class)).isZero();
    }

    @Test
    void shouldRollbackDomainChangeWhenOutboxInsertFails() {
        jdbcTemplate.execute("ALTER TABLE AUDIT_OUTBOX ADD CONSTRAINT CK_AUDIT_OUTBOX_TEST CHECK (status <> 'PENDING')");

        assertThatThrownBy(() -> useCase.execute(false)).isInstanceOf(RuntimeException.class);

        assertThat(jdbcTemplate.queryForObject("SELECT COUNT(*) FROM DOMAIN_CHANGE", Integer.class)).isZero();
        assertThat(jdbcTemplate.queryForObject("SELECT COUNT(*) FROM AUDIT_OUTBOX", Integer.class)).isZero();
    }

    @Entity
    @Table(name = "audit_outbox", uniqueConstraints = @UniqueConstraint(
            name = "UK_AUDIT_OUTBOX_IDENTIFIER", columnNames = "identifier"))
    public static class TestAuditOutboxEntity extends AuditOutboxEntity {
        protected TestAuditOutboxEntity() {
        }

    }

    @SpringBootConfiguration
    @EnableAutoConfiguration
    @EntityScan(basePackageClasses = AuditOutboxTransactionIT.class)
    @Import(TestBeans.class)
    static class TestApplication {
    }

    @Configuration(proxyBeanMethods = false)
    static class TestBeans {
        @Bean
        AuditAuthorizationContextResolver auditAuthorizationContextResolver() {
            AuditAuthorizationContextResolver resolver = mock(AuditAuthorizationContextResolver.class);
            when(resolver.resolve()).thenReturn(
                    new AuditContext("account-1", "application-1", "DEV", "user-1", "correlation-1"));
            return resolver;
        }

        @Bean
        AuditedUseCase auditedUseCase(JdbcTemplate jdbcTemplate) {
            return new AuditedUseCase(jdbcTemplate);
        }
    }

    static class AuditedUseCase {
        private final JdbcTemplate jdbcTemplate;

        AuditedUseCase(JdbcTemplate jdbcTemplate) {
            this.jdbcTemplate = jdbcTemplate;
        }

        @Transactional
        @Auditable(action = AuditAction.CREATE, event = "CREATED", resourceType = "KEY")
        public Map<String, String> execute(boolean failAfterWrite) {
            jdbcTemplate.update("INSERT INTO DOMAIN_CHANGE (IDENTIFIER) VALUES (?)", "resource-1");
            if (failAfterWrite) {
                throw new IllegalStateException("business failure");
            }
            return Map.of("identifier", "resource-1", "name", "timeout");
        }
    }
}
