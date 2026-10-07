CREATE TABLE audit_outbox (
    id BIGINT NOT NULL AUTO_INCREMENT,
    identifier VARCHAR(36) NOT NULL,
    payload JSON NOT NULL,
    metadata JSON NOT NULL,
    status VARCHAR(20) NOT NULL DEFAULT 'PENDING',
    claimed_at DATETIME(6) NULL,
    claimed_by VARCHAR(100) NULL,
    attempts INT NOT NULL DEFAULT 0,
    next_attempt_at DATETIME(6) NULL,
    created_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    processed_at DATETIME(6) NULL,
    CONSTRAINT PK_AUDIT_OUTBOX PRIMARY KEY (id),
    CONSTRAINT UK_AUDIT_OUTBOX_IDENTIFIER UNIQUE (identifier),
    INDEX IDX_AUDIT_OUTBOX_STATUS_CREATED (status, created_at, id),
    INDEX IDX_AUDIT_OUTBOX_RETRY (status, next_attempt_at)
) ENGINE=InnoDB;
