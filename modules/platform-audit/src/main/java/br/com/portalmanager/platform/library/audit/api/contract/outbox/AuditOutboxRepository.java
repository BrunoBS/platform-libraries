package br.com.portalmanager.platform.library.audit.api.contract.outbox;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.repository.NoRepositoryBean;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.time.Instant;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import br.com.portalmanager.platform.library.audit.api.contract.model.AuditOutboxStatus;

/** Generic storage contract for a service-owned audit outbox entity. */
@NoRepositoryBean
public interface AuditOutboxRepository<E extends AuditOutboxEntity> extends JpaRepository<E, Long> {

    Optional<E> findByIdentifier(String identifier);

    long deleteByIdentifier(String identifier);

    List<E> findByStatusInAndNextAttemptAtLessThanEqualOrderByCreatedAtAscIdAsc(
            Collection<AuditOutboxStatus> statuses, Instant now, Pageable pageable);

    List<E> findByStatusAndCreatedAtLessThanEqualAndNextAttemptAtIsNullOrderByCreatedAtAscIdAsc(
            AuditOutboxStatus status, Instant createdBefore, Pageable pageable);

    List<E> findByStatusAndAttemptsLessThanAndClaimedAtLessThanEqualOrderByCreatedAtAscIdAsc(
            AuditOutboxStatus status, int attempts, Instant claimedBefore, Pageable pageable);

    List<E> findByStatusAndAttemptsGreaterThanEqualAndClaimedAtLessThanEqualOrderByCreatedAtAscIdAsc(
            AuditOutboxStatus status, int attempts, Instant claimedBefore, Pageable pageable);

    List<E> findByStatusInAndNextAttemptAtIsNullOrderByCreatedAtAscIdAsc(
            Collection<AuditOutboxStatus> statuses, Pageable pageable);

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("update #{#entityName} e set e.status = :processing, "
            + "e.attempts = case when e.attempts < :maximumAttempts then e.attempts + 1 else e.attempts end, "
            + "e.claimedAt = :now where e.identifier = :identifier and e.status in :eligible "
            + "and (e.status <> :processing or e.claimedAt is null or e.claimedAt <= :staleBefore) "
            + "and (e.nextAttemptAt is null or e.nextAttemptAt <= :now)")
    int claim(@Param("identifier") String identifier, @Param("eligible") Collection<AuditOutboxStatus> eligible,
              @Param("processing") AuditOutboxStatus processing, @Param("now") Instant now,
              @Param("staleBefore") Instant staleBefore, @Param("maximumAttempts") int maximumAttempts);

    @Modifying
    @Query("update #{#entityName} e set e.status = :status, e.nextAttemptAt = :next, e.claimedAt = null "
            + "where e.identifier = :identifier and e.status = :processing")
    int scheduleRetry(@Param("identifier") String identifier, @Param("processing") AuditOutboxStatus processing,
                      @Param("status") AuditOutboxStatus status, @Param("next") Instant next);

    @SuppressWarnings("unchecked")
    default void append(AuditOutboxEntity entry) {
        save((E) entry);
    }
}
