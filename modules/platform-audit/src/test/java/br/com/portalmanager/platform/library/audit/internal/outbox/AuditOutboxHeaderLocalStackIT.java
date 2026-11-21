package br.com.portalmanager.platform.library.audit.internal.outbox;

import br.com.portalmanager.platform.library.audit.api.configuration.PlatformAuditProperties;
import br.com.portalmanager.platform.library.audit.api.contract.model.AuditContext;
import br.com.portalmanager.platform.library.audit.api.annotation.Auditable;
import br.com.portalmanager.platform.library.audit.api.annotation.AuditAttribute;
import br.com.portalmanager.platform.library.audit.api.contract.model.AuditAction;
import br.com.portalmanager.platform.library.audit.internal.context.AuditAuthorizationContextResolver;
import br.com.portalmanager.platform.library.audit.internal.event.AuditEventFactory;
import br.com.portalmanager.platform.library.audit.api.contract.outbox.AuditOutboxRepository;
import br.com.portalmanager.platform.library.objectstorage.api.configuration.ObjectStorageProperties;
import br.com.portalmanager.platform.library.objectstorage.api.contract.ObjectStorageClient;
import br.com.portalmanager.platform.library.objectstorage.api.contract.model.ObjectReference;
import br.com.portalmanager.platform.library.objectstorage.api.contract.response.ObjectMetadata;
import br.com.portalmanager.platform.library.objectstorage.provider.aws.S3ObjectStorageClient;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.HexFormat;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;
import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

/** Uses an existing LocalStack, with an exclusive bucket removed on completion. */
@org.junit.jupiter.api.condition.EnabledIfSystemProperty(named = "audit.localstack.enabled", matches = "true")
class AuditOutboxHeaderLocalStackIT {
    @Test void objectContainsOnlySnapshotAndHeadReturnsCompleteMetadata() throws Exception {
        String bucket = "audit-layout-" + UUID.randomUUID();
        try (S3Client s3 = S3Client.builder()
                .endpointOverride(URI.create(System.getProperty("audit.localstack.endpoint", "http://127.0.0.1:4566")))
                .region(Region.US_EAST_1).forcePathStyle(true)
                .credentialsProvider(StaticCredentialsProvider.create(AwsBasicCredentials.create("test", "test")))
                .build()) {
            s3.createBucket(request -> request.bucket(bucket));
            PlatformAuditProperties auditProperties =
                    new PlatformAuditProperties();
            auditProperties.setServiceName("workspace-service");
            AuditAuthorizationContextResolver context =
                    mock(AuditAuthorizationContextResolver.class);
            when(context.resolve()).thenReturn(new AuditContext(
                    null, null, null, "José", "correlation"));
            AuditEventFactory factory =
                    new AuditEventFactory(
                            auditProperties, context, AuditOutboxProcessorTest.JSON);
            AuditEventFactory.CapturedAuditEvent captured = factory.create(
                    getClass().getMethod("auditableUpdate").getAnnotation(
                            Auditable.class),
                    AuditOutboxProcessorTest.JSON.readTree("{\"code\":\"resource-1\",\"name\":\"Snapshot local\",\"revision\":7,\"lifecycle\":\"ACTIVE\"}"));
            AuditOutboxProcessorTest.Entry entry = new AuditOutboxProcessorTest.Entry(captured.payload(), captured.metadata());
            String key = "platform-audit/workspace-service/resource-1.json";
            try {
                s3.putBucketVersioning(request -> request.bucket(bucket)
                        .versioningConfiguration(configuration -> configuration.status("Enabled")));
                ObjectStorageProperties properties = new ObjectStorageProperties();
                ObjectStorageProperties.Destination destination = new ObjectStorageProperties.Destination();
                destination.setBucket(bucket);
                properties.getDestinations().put("audit", destination);
                ObjectStorageClient storage = new S3ObjectStorageClient(s3, mock(S3Presigner.class), properties);
                AuditOutboxRepository<AuditOutboxProcessorTest.Entry> repository = AuditOutboxProcessorTest.repository();
                AuditOutboxProcessorTest.processor(repository, storage, entry).processNew(entry.getIdentifier());
                verify(repository).deleteByIdentifier(entry.getIdentifier());
                ObjectMetadata head = storage.head(ObjectReference.current("audit", key));
                assertThat(head.reference().versionId()).isNotBlank().isNotEqualTo("null");
                assertThat(AuditOutboxProcessorTest.JSON.readTree(head.metadata().get("audit-metadata"))).isEqualTo(AuditOutboxProcessorTest.JSON.readTree(AuditOutboxProcessorTest.JSON.writeValueAsString(entry.getMetadata())));
                assertThat(AuditOutboxProcessorTest.JSON.readTree(head.metadata().get("audit-metadata")).path("resourceVersion").longValue())
                        .isEqualTo(7);
                assertThat(AuditOutboxProcessorTest.JSON.readTree(head.metadata().get("audit-metadata"))
                        .path("resourceStatus").asText()).isEqualTo("ACTIVE");
                assertThat(AuditOutboxProcessorTest.JSON.readTree(head.metadata().get("audit-metadata")).has("ignored")).isFalse();
                assertThat(head.metadata()).containsEntry("audit-event-id", entry.getIdentifier())
                        .containsEntry("audit-format-version", "2")
                        .containsEntry("audit-outbox-created-at", entry.getCreatedAt().toString());
                assertThat(head.responseHeaders().keySet()).anyMatch(name -> name.equalsIgnoreCase("x-amz-meta-audit-metadata"));
                // Read only this exclusive test object to verify the producer's byte-level contract.
                byte[] snapshot = s3.getObjectAsBytes(request -> request.bucket(bucket).key(key)).asByteArray();
                assertThat(AuditOutboxProcessorTest.JSON.readTree(snapshot)).isEqualTo(entry.getPayload());
                assertThat(head.sizeBytes()).isEqualTo(snapshot.length);
                assertThat(head.metadata().get("audit-sha256"))
                        .isEqualTo(HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(snapshot)));
                AuditEventFactory.CapturedAuditEvent nextCaptured = factory.create(
                        getClass().getMethod("auditableUpdate").getAnnotation(Auditable.class),
                        AuditOutboxProcessorTest.JSON.readTree("{\"code\":\"resource-1\",\"name\":\"Second snapshot\",\"revision\":8,\"lifecycle\":\"INACTIVE\"}"));
                AuditOutboxProcessorTest.Entry next = new AuditOutboxProcessorTest.Entry(nextCaptured.payload(), nextCaptured.metadata());
                AuditOutboxRepository<AuditOutboxProcessorTest.Entry> nextRepository = AuditOutboxProcessorTest.repository();
                AuditOutboxProcessorTest.processor(nextRepository, storage, next).processNew(next.getIdentifier());
                verify(nextRepository).deleteByIdentifier(next.getIdentifier());
                ObjectMetadata latest = storage.head(ObjectReference.current("audit", key));
                assertThat(latest.reference().versionId()).isNotBlank().isNotEqualTo(head.reference().versionId());
                assertThat(latest.metadata().get("audit-event-id")).isEqualTo(next.getIdentifier());
                ObjectMetadata original = storage.head(new ObjectReference("audit", key, head.reference().versionId()));
                assertThat(original.metadata().get("audit-event-id")).isEqualTo(entry.getIdentifier());
                assertThat(AuditOutboxProcessorTest.JSON.readTree(original.metadata().get("audit-metadata"))
                        .path("resourceVersion").longValue()).isEqualTo(7);
                assertThat(AuditOutboxProcessorTest.JSON.readTree(latest.metadata().get("audit-metadata"))
                        .path("resourceVersion").longValue()).isEqualTo(8);
                assertThat(s3.listObjectVersions(request -> request.bucket(bucket).prefix(key)).versions()).hasSize(2);
                System.out.println("Versioned key: " + key);
                System.out.println("S3 versions: " + original.reference().versionId() + ", " + latest.reference().versionId());
                System.out.println("Original snapshot file: " + new String(snapshot, StandardCharsets.UTF_8));
            } finally {
                software.amazon.awssdk.services.s3.model.ListObjectVersionsResponse versions =
                        s3.listObjectVersions(request -> request.bucket(bucket));
                for (software.amazon.awssdk.services.s3.model.ObjectVersion version : versions.versions()) {
                    s3.deleteObject(request -> request.bucket(bucket).key(version.key()).versionId(version.versionId()));
                }
                for (software.amazon.awssdk.services.s3.model.DeleteMarkerEntry marker : versions.deleteMarkers()) {
                    s3.deleteObject(request -> request.bucket(bucket).key(marker.key()).versionId(marker.versionId()));
                }
                s3.deleteBucket(request -> request.bucket(bucket));
            }
        }
    }
    @Auditable(
            action = AuditAction.UPDATE,
            event = "WORKSPACE_UPDATED", resourceType = "WORKSPACE", attributes = {
                    @AuditAttribute(key = "resourceIdentifier", value = "code"),
                    @AuditAttribute(key = "resourceVersion", value = "revision"),
                    @AuditAttribute(key = "resourceStatus", value = "lifecycle"),
                    @AuditAttribute(key = "ignored", value = "missing")})
    public void auditableUpdate() { }
}
