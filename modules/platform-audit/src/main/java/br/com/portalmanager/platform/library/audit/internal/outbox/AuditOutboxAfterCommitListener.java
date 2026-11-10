package br.com.portalmanager.platform.library.audit.internal.outbox;

import org.springframework.core.task.TaskExecutor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

/** Starts the fast path only after the transaction that inserted the outbox row commits. */
public final class AuditOutboxAfterCommitListener {
    private static final Logger LOGGER = LoggerFactory.getLogger(AuditOutboxAfterCommitListener.class);
    private final TaskExecutor executor;
    private final AuditOutboxProcessor processor;

    public AuditOutboxAfterCommitListener(TaskExecutor executor, AuditOutboxProcessor processor) {
        this.executor = executor;
        this.processor = processor;
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onCreated(AuditOutboxCreatedEvent event) {
        try {
            executor.execute(() -> processor.processNew(event.identifier()));
        } catch (RuntimeException rejected) {
            LOGGER.warn("Audit outbox after-commit signal could not be queued; recovery scheduler will retry: eventId={}",
                    event.identifier(), rejected);
        }
    }
}
