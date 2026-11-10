package br.com.portalmanager.platform.library.audit.outbox;

import br.com.portalmanager.platform.library.audit.config.PlatformAuditProperties;
import br.com.portalmanager.platform.library.audit.model.AuditOutboxStatus;
import br.com.portalmanager.platform.library.objectstorage.api.contract.ObjectStorageClient;
import br.com.portalmanager.platform.library.objectstorage.api.contract.model.ObjectHeaders;
import br.com.portalmanager.platform.library.objectstorage.api.contract.model.ObjectReference;
import br.com.portalmanager.platform.library.objectstorage.api.contract.model.WriteMode;
import br.com.portalmanager.platform.library.objectstorage.api.contract.request.UploadRequest;
import br.com.portalmanager.platform.library.objectstorage.api.contract.response.ObjectMetadata;
import br.com.portalmanager.platform.library.objectstorage.api.contract.response.UploadResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.node.ObjectNode;

import java.io.ByteArrayInputStream;
import java.security.MessageDigest;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Base64;
import java.util.HexFormat;
import java.util.List;
import java.util.Map;

/** Uploads committed audit events and retries failed uploads in bounded batches. */
public final class AuditOutboxProcessor {
    private static final Logger LOGGER = LoggerFactory.getLogger(AuditOutboxProcessor.class);
    private static final List<AuditOutboxStatus> FAST_STATUSES =
            List.of(AuditOutboxStatus.PENDING, AuditOutboxStatus.RETRY, AuditOutboxStatus.PROCESSING);
    private static final List<AuditOutboxStatus> DAILY_STATUSES =
            List.of(AuditOutboxStatus.ERROR, AuditOutboxStatus.PROCESSING);

    private final AuditOutboxRepository<?> repository;
    private final ObjectStorageClient objectStorageClient;
    private final PlatformAuditProperties properties;
    private final ObjectMapper objectMapper;
    private final TransactionTemplate transaction;

    public AuditOutboxProcessor(
            AuditOutboxRepository<?> repository,
            ObjectStorageClient objectStorageClient,
            PlatformAuditProperties properties,
            ObjectMapper objectMapper,
            PlatformTransactionManager transactionManager) {
        this.repository = repository;
        this.objectStorageClient = objectStorageClient;
        this.properties = properties;
        this.objectMapper = objectMapper;
        this.transaction = new TransactionTemplate(transactionManager);
    }

    /** Processes the newly committed row only. */
    public void processNew(String identifier) {
        process(identifier, List.of(AuditOutboxStatus.PENDING));
    }

    /** Retries recent failures and stale pending or interrupted short attempts. */
    public void processShortRetryBatch() {
        try {
            Instant now = Instant.now();
            Pageable page = firstPage();
            List<String> identifiers = new ArrayList<>();
            identifiers.addAll(repository.findByStatusInAndNextAttemptAtLessThanEqualOrderByCreatedAtAscIdAsc(
                    List.of(AuditOutboxStatus.RETRY), now, page).stream()
                    .map(AuditOutboxEntity::getIdentifier).toList());

            int remaining = properties.getOutbox().getBatchSize() - identifiers.size();
            if (remaining > 0) {
                identifiers.addAll(repository.findByStatusAndCreatedAtLessThanEqualAndNextAttemptAtIsNullOrderByCreatedAtAscIdAsc(
                        AuditOutboxStatus.PENDING, now.minus(properties.getOutbox().getShortRetryDelay()),
                        page(remaining)).stream().map(AuditOutboxEntity::getIdentifier).toList());
            }

            remaining = properties.getOutbox().getBatchSize() - identifiers.size();
            if (remaining > 0) {
                identifiers.addAll(repository.findByStatusAndAttemptsLessThanAndClaimedAtLessThanEqualOrderByCreatedAtAscIdAsc(
                        AuditOutboxStatus.PROCESSING, properties.getOutbox().getInitialAttempts(),
                        now.minus(properties.getOutbox().getProcessingLease()), page(remaining)).stream()
                        .map(AuditOutboxEntity::getIdentifier).toList());
            }
            processBatch(identifiers, FAST_STATUSES);
        } catch (RuntimeException failure) {
            LOGGER.error("Audit outbox short-retry batch failed", failure);
        }
    }

    /** Retries older failures once per day and recovers interrupted daily attempts. */
    public void processDailyRetryBatch() {
        try {
            Instant now = Instant.now();
            List<String> identifiers = new ArrayList<>(repository
                    .findByStatusInAndNextAttemptAtLessThanEqualOrderByCreatedAtAscIdAsc(
                            List.of(AuditOutboxStatus.ERROR), now, firstPage())
                    .stream().map(AuditOutboxEntity::getIdentifier).toList());

            int remaining = properties.getOutbox().getBatchSize() - identifiers.size();
            if (remaining > 0) {
                identifiers.addAll(repository.findByStatusAndAttemptsGreaterThanEqualAndClaimedAtLessThanEqualOrderByCreatedAtAscIdAsc(
                        AuditOutboxStatus.PROCESSING, properties.getOutbox().getInitialAttempts(),
                        now.minus(properties.getOutbox().getProcessingLease()), page(remaining)).stream()
                        .map(AuditOutboxEntity::getIdentifier).toList());
            }
            processBatch(identifiers, DAILY_STATUSES);
        } catch (RuntimeException failure) {
            LOGGER.error("Audit outbox daily-retry batch failed", failure);
        }
    }

    public void processFastBatch() {
        processShortRetryBatch();
    }

    public void processDailyBatch() {
        processDailyRetryBatch();
    }

    private void processBatch(List<String> identifiers, List<AuditOutboxStatus> eligibleStatuses) {
        for (String identifier : identifiers) {
            process(identifier, eligibleStatuses);
        }
    }

    private void process(String identifier, List<AuditOutboxStatus> eligibleStatuses) {
        try {
            Instant now = Instant.now();
            Duration processingLease = properties.getOutbox().getProcessingLease();
            ClaimResult claim = transaction.execute(status -> {
                AuditOutboxEntity beforeClaim = repository.findByIdentifier(identifier).orElse(null);
                if (beforeClaim == null) {
                    return null;
                }
                int updated = repository.claim(
                        identifier,
                        eligibleStatuses,
                        AuditOutboxStatus.PROCESSING,
                        now,
                        now.minus(processingLease),
                        properties.getOutbox().getMaximumAttempts());
                AuditOutboxEntity claimed = updated == 1 ? repository.findByIdentifier(identifier).orElse(null) : null;
                return claimed == null ? null : new ClaimResult(claimed, beforeClaim.getAttempts());
            });
            if (claim == null) {
                return;
            }

            AuditOutboxEntity claimed = claim.entry();
            if (claim.attemptsBeforeClaim() >= properties.getOutbox().getMaximumAttempts()
                    || !now.isBefore(claimed.getCreatedAt().plusDays(properties.getOutbox().getMaximumRecoveryDays()))) {
                discard(claimed, "retry_limit_or_recovery_window_expired");
                return;
            }
            upload(claimed);
        } catch (RuntimeException failure) {
            LOGGER.error("Audit outbox processing failed: eventId={}", identifier, failure);
        }
    }

    private void upload(AuditOutboxEntity entry) {
        byte[] content;
        String sha256;
        try {
            ObjectNode storedEvent = objectMapper.createObjectNode();
            storedEvent.put("identifier", entry.getIdentifier());
            storedEvent.set("payload", entry.getPayload());
            storedEvent.set("metadata", entry.getMetadata());
            content = objectMapper.writeValueAsBytes(storedEvent);
            sha256 = HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(content));
        } catch (Exception failure) {
            failed(entry, failure);
            return;
        }

        String key = objectKey(entry.getIdentifier());
        ObjectReference reference = ObjectReference.current(properties.getOutbox().getDestination(), key);
        try {
            UploadRequest request = new UploadRequest(
                    properties.getOutbox().getDestination(),
                    key,
                    content.length,
                    new ObjectHeaders("application/json", null, null, null, null),
                    Map.of("audit-event-id", entry.getIdentifier(), "audit-sha256", sha256),
                    WriteMode.CREATE_ONLY);
            UploadResult result = objectStorageClient.upload(request, new ByteArrayInputStream(content));
            if (result == null || result.sizeBytes() != content.length) {
                throw new IllegalStateException("Object Storage returned an invalid upload result");
            }
            remove(entry.getIdentifier());
        } catch (RuntimeException failure) {
            if (storedObjectMatches(reference, entry.getIdentifier(), content.length, sha256, failure)) {
                remove(entry.getIdentifier());
                return;
            }
            failed(entry, failure);
        }
    }

    private boolean storedObjectMatches(
            ObjectReference reference,
            String identifier,
            long contentLength,
            String sha256,
            RuntimeException uploadFailure) {
        try {
            if (!objectStorageClient.exists(reference)) {
                return false;
            }
            ObjectMetadata metadata = objectStorageClient.head(reference);
            return metadata.sizeBytes() == contentLength
                    && identifier.equals(metadata.metadata().get("audit-event-id"))
                    && sha256.equals(metadata.metadata().get("audit-sha256"));
        } catch (RuntimeException verificationFailure) {
            uploadFailure.addSuppressed(verificationFailure);
            return false;
        }
    }

    private void failed(AuditOutboxEntity entry, Exception failure) {
        int attempts = entry.getAttempts();
        if (attempts >= properties.getOutbox().getMaximumAttempts()
                || !Instant.now().isBefore(entry.getCreatedAt().plusDays(properties.getOutbox().getMaximumRecoveryDays()))) {
            discard(entry, failure.getClass().getSimpleName());
            return;
        }

        boolean dailyRetry = attempts >= properties.getOutbox().getInitialAttempts();
        AuditOutboxStatus nextStatus = dailyRetry ? AuditOutboxStatus.ERROR : AuditOutboxStatus.RETRY;
        Duration delay = dailyRetry
                ? properties.getOutbox().getDailyRetryDelay()
                : properties.getOutbox().getShortRetryDelay();
        transaction.executeWithoutResult(status -> repository.scheduleRetry(
                entry.getIdentifier(), AuditOutboxStatus.PROCESSING, nextStatus, Instant.now().plus(delay)));
        LOGGER.warn("Audit outbox upload failed: eventId={}, resourceId={}, createdAt={}, attempt={}, nextStatus={}, errorType={}",
                entry.getIdentifier(), resourceIdentifier(entry.getMetadata()), entry.getCreatedAt(), attempts,
                nextStatus, failure.getClass().getSimpleName());
    }

    private void remove(String identifier) {
        transaction.executeWithoutResult(status -> repository.deleteByIdentifier(identifier));
    }

    private void discard(AuditOutboxEntity entry, String reason) {
        transaction.executeWithoutResult(status -> repository.deleteByIdentifier(entry.getIdentifier()));
        LOGGER.error("Audit outbox event discarded: eventId={}, resourceId={}, createdAt={}, discardedAt={}, attempts={}, reason={}",
                entry.getIdentifier(), resourceIdentifier(entry.getMetadata()), entry.getCreatedAt(), Instant.now(),
                entry.getAttempts(), reason);
    }

    private String objectKey(String identifier) {
        String prefix = properties.getOutbox().getKeyPrefix();
        String separator = prefix.endsWith("/") ? "" : "/";
        return prefix + separator + identifier + ".json";
    }

    private String resourceIdentifier(JsonNode metadata) {
        JsonNode identifier = metadata == null ? null : metadata.get("resourceIdentifier");
        return identifier == null || identifier.isNull() ? null : identifier.asText();
    }

    private Pageable firstPage() {
        return page(properties.getOutbox().getBatchSize());
    }

    private Pageable page(int size) {
        return PageRequest.of(0, size, Sort.by("createdAt").ascending().and(Sort.by("id").ascending()));
    }

    private record ClaimResult(AuditOutboxEntity entry, int attemptsBeforeClaim) { }
}
