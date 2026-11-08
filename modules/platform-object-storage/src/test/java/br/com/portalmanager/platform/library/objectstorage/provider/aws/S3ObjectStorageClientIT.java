package br.com.portalmanager.platform.library.objectstorage.provider.aws;

import br.com.portalmanager.platform.library.objectstorage.api.configuration.ObjectStorageProperties;
import br.com.portalmanager.platform.library.objectstorage.api.configuration.ObjectStorageProvider;
import br.com.portalmanager.platform.library.objectstorage.api.contract.response.DownloadedObject;
import br.com.portalmanager.platform.library.objectstorage.api.contract.model.ObjectHeaders;
import br.com.portalmanager.platform.library.objectstorage.api.contract.model.ObjectReference;
import br.com.portalmanager.platform.library.objectstorage.api.contract.ObjectStorageClient;
import br.com.portalmanager.platform.library.objectstorage.api.contract.request.DownloadUrlRequest;
import br.com.portalmanager.platform.library.objectstorage.api.contract.response.SignedUrl;
import br.com.portalmanager.platform.library.objectstorage.api.contract.SignedUrlClient;
import br.com.portalmanager.platform.library.objectstorage.api.contract.request.UploadUrlRequest;
import br.com.portalmanager.platform.library.objectstorage.api.contract.request.UploadRequest;
import br.com.portalmanager.platform.library.objectstorage.api.contract.response.UploadResult;
import br.com.portalmanager.platform.library.objectstorage.api.contract.model.WriteMode;
import br.com.portalmanager.platform.library.messaging.exception.ConflictException;
import br.com.portalmanager.platform.library.messaging.exception.NotFoundException;
import br.com.portalmanager.platform.library.testing.cloud.aws.annotation.WithAwsLocalStack;
import br.com.portalmanager.platform.library.testing.cloud.aws.AwsLocalStackConnection;
import br.com.portalmanager.platform.library.testing.cloud.aws.s3.annotation.AwsS3;
import java.io.ByteArrayInputStream;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.S3Configuration;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;
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

    @Autowired
    private SignedUrlClient signedUrls;

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
        TrackingInputStream firstStream = new TrackingInputStream(firstContent);
        UploadResult first = storage.upload(
                new UploadRequest("audit", KEY, firstContent.length,
                        new ObjectHeaders("application/json", "no-cache", "inline", "identity", "en"),
                        Map.of("revision", "first"), WriteMode.CREATE_ONLY),
                firstStream);
        assertFalse(firstStream.closed());
        firstStream.close();
        assertTrue(firstStream.closed());
        assertNotNull(first.reference().versionId());
        assertEquals("p5N7ZLjKpY8Dchu2us9ceMsjX+vg5wsbhM2ZVBRhoI4=", first.sha256());

        TrackingInputStream duplicateStream = new TrackingInputStream(firstContent);
        assertThrows(ConflictException.class, () -> storage.upload(
                new UploadRequest("audit", KEY, firstContent.length, "application/json", Map.of("revision", "duplicate")),
                duplicateStream));
        assertFalse(duplicateStream.closed());
        duplicateStream.close();

        byte[] secondContent = "second".getBytes(StandardCharsets.UTF_8);
        UploadResult second = storage.upload(
                new UploadRequest("audit", KEY, secondContent.length,
                        new ObjectHeaders("application/json", "no-cache", "inline", "identity", "en"),
                        Map.of("revision", "second"), WriteMode.OVERWRITE),
                new ByteArrayInputStream(secondContent));
        assertNotNull(second.reference().versionId());
        assertTrue(storage.exists(ObjectReference.current("audit", KEY)));
        assertEquals("second", storage.head(ObjectReference.current("audit", KEY)).metadata().get("revision"));
        assertEquals(new ObjectHeaders("application/json", "no-cache", "inline", "identity", "en"),
                storage.head(ObjectReference.current("audit", KEY)).headers());
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
        assertFalse(storage.exists(ObjectReference.current("audit", "missing.json")));
    }

    @Test
    void shouldAllowOnlyOneConcurrentCreateOnlyUpload() throws Exception {
        String concurrentKey = "concurrent.json";
        byte[] content = "concurrent".getBytes(StandardCharsets.UTF_8);
        CountDownLatch ready = new CountDownLatch(2);
        CountDownLatch start = new CountDownLatch(1);
        Callable<Boolean> upload = () -> {
            ready.countDown();
            if (!start.await(10, TimeUnit.SECONDS)) return false;
            try {
                storage.upload(new UploadRequest("audit", concurrentKey, content.length, "application/json", Map.of()),
                        new ByteArrayInputStream(content));
                return true;
            } catch (ConflictException exception) {
                return false;
            }
        };
        ExecutorService executor = Executors.newFixedThreadPool(2);
        try {
            List<Future<Boolean>> results = List.of(executor.submit(upload), executor.submit(upload));
            assertTrue(ready.await(10, TimeUnit.SECONDS));
            start.countDown();
            long successes = results.stream().filter(result -> get(result)).count();
            assertEquals(1, successes);
        } finally {
            start.countDown();
            executor.shutdownNow();
            storage.delete(ObjectReference.current("audit", concurrentKey));
        }
    }

    @Test
    void shouldStreamLargeObjectAndReturnItsSha256() throws Exception {
        String largeKey = "large.bin";
        byte[] content = new byte[8 * 1024 * 1024];
        UploadResult uploaded = storage.upload(
                new UploadRequest("audit", largeKey, content.length, "application/octet-stream", Map.of()),
                new ByteArrayInputStream(content));

        assertEquals("La6x82CVtEsxhBCz9Oi12Yncx7sCPRQmxJLasKMFPnQ=", uploaded.sha256());
        try (DownloadedObject downloaded = storage.download(uploaded.reference())) {
            assertEquals(content.length, downloaded.content().readAllBytes().length);
        }
        storage.delete(ObjectReference.current("audit", largeKey));
    }

    @Test
    void shouldCreateTemporaryUrlsForExactGetAndPutOperations() throws Exception {
        byte[] original = "signed get".getBytes(StandardCharsets.UTF_8);
        UploadResult uploaded = storage.upload(
                new UploadRequest("audit", "signed-get.json", original.length, "application/json", Map.of()),
                new ByteArrayInputStream(original));

        SignedUrl download = signedUrls.createDownloadUrl(new DownloadUrlRequest(
                uploaded.reference(), Duration.ofMinutes(5)));
        HttpResponse<byte[]> downloadResponse = HttpClient.newHttpClient().send(
                HttpRequest.newBuilder(download.url()).GET().build(), HttpResponse.BodyHandlers.ofByteArray());
        assertEquals(200, downloadResponse.statusCode());
        assertEquals("signed get", new String(downloadResponse.body(), StandardCharsets.UTF_8));
        assertTrue(download.expiresAt().isAfter(java.time.Instant.now()));
        assertTrue(download.requiredHeaders().isEmpty());

        ObjectHeaders uploadHeaders = new ObjectHeaders("application/json", "no-cache", null, null, null);
        ObjectReference signedPutReference = ObjectReference.current("audit", "signed-put.json");
        SignedUrl upload = signedUrls.createUploadUrl(new UploadUrlRequest(
                signedPutReference, Duration.ofMinutes(5), uploadHeaders));
        assertEquals("*", upload.requiredHeaders().get("If-None-Match"));
        HttpRequest.Builder putRequest = HttpRequest.newBuilder(upload.url());
        upload.requiredHeaders().forEach(putRequest::header);
        HttpClient httpClient = HttpClient.newHttpClient();
        HttpResponse<Void> uploadResponse = httpClient.send(
                putRequest.PUT(HttpRequest.BodyPublishers.ofString("signed put")).build(),
                HttpResponse.BodyHandlers.discarding());
        assertEquals(200, uploadResponse.statusCode());

        HttpRequest.Builder duplicatePut = HttpRequest.newBuilder(upload.url());
        upload.requiredHeaders().forEach(duplicatePut::header);
        HttpResponse<Void> duplicateResponse = httpClient.send(
                duplicatePut.PUT(HttpRequest.BodyPublishers.ofString("must not replace")).build(),
                HttpResponse.BodyHandlers.discarding());
        assertEquals(412, duplicateResponse.statusCode());

        SignedUrl overwrite = signedUrls.createUploadUrl(new UploadUrlRequest(
                signedPutReference, Duration.ofMinutes(5), uploadHeaders, WriteMode.OVERWRITE));
        assertFalse(overwrite.requiredHeaders().containsKey("If-None-Match"));
        HttpRequest.Builder overwritePut = HttpRequest.newBuilder(overwrite.url());
        overwrite.requiredHeaders().forEach(overwritePut::header);
        HttpResponse<Void> overwriteResponse = httpClient.send(
                overwritePut.PUT(HttpRequest.BodyPublishers.ofString("signed overwrite")).build(),
                HttpResponse.BodyHandlers.discarding());
        assertEquals(200, overwriteResponse.statusCode());

        try (DownloadedObject downloaded = storage.download(signedPutReference)) {
            assertEquals("signed overwrite", new String(downloaded.content().readAllBytes(), StandardCharsets.UTF_8));
        }
        storage.delete(ObjectReference.current("audit", "signed-get.json"));
        storage.delete(ObjectReference.current("audit", "signed-put.json"));
    }

    private boolean get(Future<Boolean> result) {
        try {
            return result.get(20, TimeUnit.SECONDS);
        } catch (Exception exception) {
            throw new AssertionError("Concurrent upload did not complete", exception);
        }
    }

    private static final class TrackingInputStream extends ByteArrayInputStream {
        private boolean closed;

        private TrackingInputStream(byte[] content) {
            super(content);
        }

        @Override
        public void close() {
            closed = true;
        }

        private boolean closed() {
            return closed;
        }
    }

    @Configuration(proxyBeanMethods = false)
    @WithAwsLocalStack(s3 = @AwsS3(buckets = BUCKET))
    static class LocalStackConfiguration {
        @Bean(destroyMethod = "close")
        S3Presigner s3Presigner(AwsLocalStackConnection connection) {
            return S3Presigner.builder().endpointOverride(connection.endpoint()).region(connection.region())
                    .credentialsProvider(connection.credentialsProvider())
                    .serviceConfiguration(S3Configuration.builder().pathStyleAccessEnabled(true).build())
                    .build();
        }

        @Bean
        S3ObjectStorageClient objectStorageClient(S3Client s3Client, S3Presigner presigner) {
            ObjectStorageProperties properties = new ObjectStorageProperties();
            properties.setProvider(ObjectStorageProvider.AWS);
            properties.setRegion("us-east-1");
            ObjectStorageProperties.Destination destination = new ObjectStorageProperties.Destination();
            destination.setBucket(BUCKET);
            destination.setPrefix("objects/");
            destination.setSuffix(".data");
            ObjectStorageProperties.Destination unavailable = new ObjectStorageProperties.Destination();
            unavailable.setBucket("object-storage-missing-bucket");
            properties.setDestinations(Map.of("audit", destination, "unavailable", unavailable));
            return new S3ObjectStorageClient(s3Client, presigner, properties);
        }
    }
}
