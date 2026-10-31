package br.com.portalmanager.platform.library.objectstorage.provider.aws;

import br.com.portalmanager.platform.library.objectstorage.configuration.ObjectStorageProperties;
import br.com.portalmanager.platform.library.objectstorage.configuration.ObjectStorageProvider;
import br.com.portalmanager.platform.library.objectstorage.contract.DownloadedObject;
import br.com.portalmanager.platform.library.objectstorage.contract.ObjectReference;
import br.com.portalmanager.platform.library.objectstorage.contract.ObjectStorageClient;
import br.com.portalmanager.platform.library.objectstorage.contract.UploadRequest;
import br.com.portalmanager.platform.library.objectstorage.contract.UploadResult;
import br.com.portalmanager.platform.library.objectstorage.contract.WriteMode;
import br.com.portalmanager.platform.library.messaging.exception.ConflictException;
import br.com.portalmanager.platform.library.testing.cloud.aws.annotation.WithAwsLocalStack;
import br.com.portalmanager.platform.library.testing.cloud.aws.s3.annotation.AwsS3;
import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.BucketVersioningStatus;
import software.amazon.awssdk.services.s3.model.PutBucketVersioningRequest;
import software.amazon.awssdk.services.s3.model.VersioningConfiguration;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

@SpringJUnitConfig(S3ObjectStorageClientIT.LocalStackConfiguration.class)
class S3ObjectStorageClientIT {
    private static final String BUCKET = "object-storage-contract";
    private static final String KEY = "sample.json";

    @Autowired
    private S3Client s3Client;

    @Autowired
    private ObjectStorageClient storage;

    @BeforeEach
    void enableBucketVersioning() {
        s3Client.putBucketVersioning(PutBucketVersioningRequest.builder()
                .bucket(BUCKET)
                .versioningConfiguration(VersioningConfiguration.builder()
                        .status(BucketVersioningStatus.ENABLED)
                        .build())
                .build());
    }

    @Test
    void shouldSupportVersionedObjectLifecycleThroughPublicContract() throws Exception {
        byte[] firstContent = "first".getBytes(StandardCharsets.UTF_8);
        UploadResult first = storage.upload(
                new UploadRequest("audit", KEY, firstContent.length, "application/json", Map.of("revision", "first")),
                new ByteArrayInputStream(firstContent));
        assertNotNull(first.reference().versionId());

        assertThrows(ConflictException.class, () -> storage.upload(
                new UploadRequest("audit", KEY, firstContent.length, "application/json", Map.of("revision", "duplicate")),
                new ByteArrayInputStream(firstContent)));

        byte[] secondContent = "second".getBytes(StandardCharsets.UTF_8);
        UploadResult second = storage.upload(
                new UploadRequest("audit", KEY, secondContent.length, "application/json", Map.of("revision", "second"), WriteMode.OVERWRITE),
                new ByteArrayInputStream(secondContent));
        assertNotNull(second.reference().versionId());
        assertTrue(storage.exists(ObjectReference.current("audit", KEY)));
        assertEquals("second", storage.head(ObjectReference.current("audit", KEY)).metadata().get("revision"));
        assertEquals(second.reference().versionId(), storage.head(ObjectReference.current("audit", KEY)).reference().versionId());

        try (DownloadedObject downloaded = storage.download(first.reference())) {
            assertEquals("first", new String(downloaded.content().readAllBytes(), StandardCharsets.UTF_8));
        }
        try (DownloadedObject downloaded = storage.download(ObjectReference.current("audit", KEY))) {
            assertEquals(second.reference().versionId(), downloaded.metadata().reference().versionId());
            assertEquals("second", new String(downloaded.content().readAllBytes(), StandardCharsets.UTF_8));
        }

        assertEquals(1, storage.list("audit", "sample", null, 10).objects().size());
        assertEquals(2, storage.listVersions("audit", KEY, null, 10).versions().size());
        assertEquals(second.reference().versionId(), storage.findVersionByETag("audit", KEY, second.eTag())
                .orElseThrow().reference().versionId());

        storage.deleteVersion(first.reference());
        storage.delete(ObjectReference.current("audit", KEY));
        assertFalse(storage.exists(ObjectReference.current("audit", KEY)));
    }

    @Configuration(proxyBeanMethods = false)
    @WithAwsLocalStack(s3 = @AwsS3(buckets = BUCKET))
    static class LocalStackConfiguration {
        @Bean
        ObjectStorageClient objectStorageClient(S3Client s3Client) {
            ObjectStorageProperties properties = new ObjectStorageProperties();
            properties.setProvider(ObjectStorageProvider.AWS);
            properties.setRegion("us-east-1");
            ObjectStorageProperties.Destination destination = new ObjectStorageProperties.Destination();
            destination.setBucket(BUCKET);
            properties.setDestinations(Map.of("audit", destination));
            return new S3ObjectStorageClient(s3Client, properties);
        }
    }
}
