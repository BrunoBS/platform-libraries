package br.com.portalmanager.platform.library.audit.outbox;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.repository.NoRepositoryBean;

/** Generic storage contract for a service-owned audit outbox entity. */
@NoRepositoryBean
public interface AuditOutboxRepository<E extends AuditOutboxEntity> extends JpaRepository<E, Long> {

    @SuppressWarnings("unchecked")
    default void append(AuditOutboxEntity entry) {
        save((E) entry);
    }
}
