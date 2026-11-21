package br.com.portalmanager.platform.library.audit.internal.outbox;

import br.com.portalmanager.platform.library.audit.api.configuration.PlatformAuditProperties;
import br.com.portalmanager.platform.library.audit.api.contract.model.AuditOutboxStatus;
import br.com.portalmanager.platform.library.audit.api.contract.outbox.AuditOutboxEntity;
import br.com.portalmanager.platform.library.audit.api.contract.outbox.AuditOutboxRepository;
import br.com.portalmanager.platform.library.objectstorage.api.contract.ObjectStorageClient;
import br.com.portalmanager.platform.library.objectstorage.api.contract.model.ObjectReference;
import br.com.portalmanager.platform.library.objectstorage.api.contract.request.UploadRequest;
import br.com.portalmanager.platform.library.objectstorage.api.contract.response.ObjectMetadata;
import br.com.portalmanager.platform.library.objectstorage.api.contract.response.UploadResult;
import java.io.InputStream;
import java.security.MessageDigest;
import java.time.Instant;
import java.util.HashMap;
import java.util.HexFormat;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.support.SimpleTransactionStatus;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;
import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class AuditOutboxProcessorTest {
    static final JsonMapper JSON = JsonMapper.builder().enable(tools.jackson.databind.SerializationFeature.INDENT_OUTPUT).build();
    final ObjectStorageClient storage = mock(ObjectStorageClient.class);
    final AuditOutboxRepository<Entry> repository = repository();
    final Entry entry = new Entry(JSON.readTree("{\"identifier\":\"resource-1\",\"name\":\"Snapshot\",\"metadata\":{\"domain\":true}}"),
            JSON.readTree("{\"service\":\"workspace-service\",\"resourceType\":\"WORKSPACE\",\"eventType\":\"CREATED\","
                    + "\"resourceIdentifier\":\"resource-1\",\"username\":\"José\\r\\nTeste\",\"accountIdentifier\":null,"
                    + "\"occurredAt\":\"2026-11-21T19:17:14Z\"}"));

    @SuppressWarnings("unchecked")
    static AuditOutboxRepository<Entry> repository() { return mock(AuditOutboxRepository.class); }

    static AuditOutboxProcessor processor(AuditOutboxRepository<Entry> repository, ObjectStorageClient storage, Entry entry) {
        when(repository.findByIdentifier(entry.getIdentifier())).thenReturn(Optional.of(entry));
        when(repository.claim(eq(entry.getIdentifier()), anyList(), eq(AuditOutboxStatus.PROCESSING),
                any(Instant.class), any(Instant.class), anyInt())).thenReturn(1);
        PlatformTransactionManager transactions = mock(PlatformTransactionManager.class);
        when(transactions.getTransaction(any(TransactionDefinition.class))).thenAnswer(ignored -> new SimpleTransactionStatus());
        PlatformAuditProperties properties = new PlatformAuditProperties();
        properties.getOutbox().setDestination("audit");
        return new AuditOutboxProcessor(repository, storage, properties, JSON, transactions);
    }

    static final class Entry extends AuditOutboxEntity {
        Entry(JsonNode payload, JsonNode metadata) {
            prepareForPersistence(payload, metadata);
            initializeTechnicalFields();
        }
    }

    @Test void uploadsOnlySnapshotWithCompleteAsciiMetadataAndHashOfActualFile() throws Exception {
        byte[][] uploaded = new byte[1][];
        when(storage.upload(any(UploadRequest.class), any(InputStream.class))).thenAnswer(call -> {
            UploadRequest request = call.getArgument(0);
            uploaded[0] = ((InputStream) call.getArgument(1)).readAllBytes();
            return new UploadResult(new ObjectReference("audit", request.key(), "version-1"), "etag", uploaded[0].length);
        });
        processor(repository, storage, entry).processNew(entry.getIdentifier());
        ArgumentCaptor<UploadRequest> request = ArgumentCaptor.forClass(UploadRequest.class);
        verify(storage).upload(request.capture(), any(InputStream.class));
        assertThat(JSON.readTree(uploaded[0])).isEqualTo(entry.getPayload());
        assertThat(request.getValue().contentLength()).isEqualTo(uploaded[0].length);
        assertThat(request.getValue().contentType()).isEqualTo("application/json");
        assertThat(request.getValue().key()).isEqualTo("platform-audit/workspace-service/resource-1.json");
        assertThat(request.getValue().writeMode()).isEqualTo(
                br.com.portalmanager.platform.library.objectstorage.api.contract.model.WriteMode.OVERWRITE);
        Map<String, String> headers = request.getValue().metadata();
        assertThat(headers).containsEntry("audit-format-version", "2")
                .containsEntry("audit-event-id", entry.getIdentifier())
                .containsEntry("audit-outbox-created-at", entry.getCreatedAt().toString())
                .containsEntry("audit-sha256", HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(uploaded[0])));
        assertThat(JSON.readTree(headers.get("audit-metadata"))).isEqualTo(entry.getMetadata());
        assertThat(headers.get("audit-metadata")).contains("\\u00E9");
        assertThat(headers.values()).allMatch(value -> value.chars().allMatch(c -> c >= 32 && c < 127));
        verify(repository).deleteByIdentifier(entry.getIdentifier());
    }

    @Test void oversizedMetadataIsNotTruncatedUploadedOrAcknowledged() {
        Entry oversized = new Entry(entry.getPayload(), JSON.createObjectNode().put("username", "a".repeat(2048)));
        processor(repository, storage, oversized).processNew(oversized.getIdentifier());
        verifyNoInteractions(storage);
        verify(repository, never()).deleteByIdentifier(anyString());
        verify(repository).scheduleRetry(eq(oversized.getIdentifier()), eq(AuditOutboxStatus.PROCESSING),
                eq(AuditOutboxStatus.RETRY), any(Instant.class));
    }

    @Test void missingBusinessMetadataDoesNotUploadOrDeleteOutbox() {
        Entry missing = new Entry(entry.getPayload(), null);
        processor(repository, storage, missing).processNew(missing.getIdentifier());
        verifyNoInteractions(storage);
        verify(repository, never()).deleteByIdentifier(anyString());
    }

    @Test void recoveryRequiresAllHeadersEvenWhenSnapshotHashAndIdMatch() {
        recovery(false);
        verify(repository, never()).deleteByIdentifier(anyString());
        verify(repository).scheduleRetry(eq(entry.getIdentifier()), eq(AuditOutboxStatus.PROCESSING),
                eq(AuditOutboxStatus.RETRY), any(Instant.class));
    }

    @Test void recoveryAcknowledgesCompleteMatchingSnapshotAndMetadata() {
        recovery(true);
        verify(repository).deleteByIdentifier(entry.getIdentifier());
    }

    @Test void absentResourceIdentifierDoesNotUploadUnderAnEventIdFallback() {
        Entry missing = new Entry(entry.getPayload(), JSON.createObjectNode().put("service", "workspace-service"));
        processor(repository, storage, missing).processNew(missing.getIdentifier());
        verifyNoInteractions(storage);
        verify(repository, never()).deleteByIdentifier(anyString());
        verify(repository).scheduleRetry(eq(missing.getIdentifier()), eq(AuditOutboxStatus.PROCESSING),
                eq(AuditOutboxStatus.RETRY), any(Instant.class));
    }

    private void recovery(boolean matchingMetadata) {
        when(storage.upload(any(UploadRequest.class), any(InputStream.class))).thenAnswer(call -> {
            UploadRequest request = call.getArgument(0);
            Map<String, String> headers = new HashMap<>(request.metadata());
            if (!matchingMetadata) headers.put("audit-metadata", "{}");
            when(storage.exists(any(ObjectReference.class))).thenReturn(true);
            when(storage.head(any(ObjectReference.class))).thenReturn(new ObjectMetadata(
                    ObjectReference.current("audit", request.key()), request.contentLength(), "application/json", "etag", null, headers));
            throw new IllegalStateException("Upload response lost");
        });
        processor(repository, storage, entry).processNew(entry.getIdentifier());
    }
}
