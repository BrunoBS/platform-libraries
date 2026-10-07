package br.com.portalmanager.platform.library.audit.outbox;

import br.com.portalmanager.platform.library.audit.model.AuditOutboxStatus;
import jakarta.persistence.Column;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.MappedSuperclass;
import jakarta.persistence.PrePersist;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.node.ObjectNode;

import java.time.Instant;
import java.util.UUID;

/** Shared persistence mapping for a service-owned audit outbox entity. */
@MappedSuperclass
public abstract class AuditOutboxEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "identifier", nullable = false, length = 36, updatable = false)
    private String identifier;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "payload", nullable = false, columnDefinition = "json")
    private ObjectNode payload;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "metadata", nullable = false, columnDefinition = "json")
    private ObjectNode metadata;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private AuditOutboxStatus status;

    @Column(name = "claimed_at")
    private Instant claimedAt;

    @Column(name = "claimed_by", length = 100)
    private String claimedBy;

    @Column(name = "attempts", nullable = false)
    private int attempts;

    @Column(name = "next_attempt_at")
    private Instant nextAttemptAt;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "processed_at")
    private Instant processedAt;

    protected AuditOutboxEntity() {
    }

    void prepareForPersistence(ObjectNode payload, ObjectNode metadata) {
        this.identifier = UUID.randomUUID().toString();
        this.payload = payload;
        this.metadata = metadata;
        this.status = AuditOutboxStatus.PENDING;
        this.attempts = 0;
    }

    @PrePersist
    protected void initializeTechnicalFields() {
        if (identifier == null) {
            identifier = UUID.randomUUID().toString();
        }
        if (status == null) {
            status = AuditOutboxStatus.PENDING;
        }
        if (createdAt == null) {
            createdAt = Instant.now();
        }
    }

    public Long getId() { return id; }
    public String getIdentifier() { return identifier; }
    public JsonNode getPayload() { return payload; }
    public JsonNode getMetadata() { return metadata; }
    public AuditOutboxStatus getStatus() { return status; }
    public Instant getClaimedAt() { return claimedAt; }
    public String getClaimedBy() { return claimedBy; }
    public int getAttempts() { return attempts; }
    public Instant getNextAttemptAt() { return nextAttemptAt; }
    public Instant getCreatedAt() { return createdAt; }
    public Instant getProcessedAt() { return processedAt; }
}
