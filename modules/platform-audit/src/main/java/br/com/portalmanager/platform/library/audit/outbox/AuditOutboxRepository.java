package br.com.portalmanager.platform.library.audit.outbox;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.repository.NoRepositoryBean;

/** Generic repository contract for a concrete service-owned audit outbox entity. */
@NoRepositoryBean
public interface AuditOutboxRepository<E extends AuditOutboxEntity> extends JpaRepository<E, Long> {
}
