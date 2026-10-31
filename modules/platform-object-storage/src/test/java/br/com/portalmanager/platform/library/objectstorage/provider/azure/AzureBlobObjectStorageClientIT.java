package br.com.portalmanager.platform.library.objectstorage.provider.azure;

import br.com.portalmanager.platform.library.objectstorage.configuration.ObjectStorageProperties;
import br.com.portalmanager.platform.library.objectstorage.configuration.ObjectStorageProvider;
import br.com.portalmanager.platform.library.objectstorage.contract.DownloadedObject;
import br.com.portalmanager.platform.library.objectstorage.contract.ObjectHeaders;
import br.com.portalmanager.platform.library.objectstorage.contract.ObjectReference;
import br.com.portalmanager.platform.library.objectstorage.contract.ObjectStorageClient;
import br.com.portalmanager.platform.library.objectstorage.contract.UploadRequest;
import br.com.portalmanager.platform.library.objectstorage.contract.UploadResult;
import br.com.portalmanager.platform.library.objectstorage.contract.WriteMode;
import br.com.portalmanager.platform.library.messaging.exception.ConflictException;
import br.com.portalmanager.platform.library.testing.cloud.azure.annotation.WithAzureEmulator;
import br.com.portalmanager.platform.library.testing.cloud.azure.blob.annotation.AzureBlobStorage;
import com.azure.storage.blob.BlobServiceClient;
import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

@SpringJUnitConfig(AzureBlobObjectStorageClientIT.AzuriteConfiguration.class)
class AzureBlobObjectStorageClientIT {
    private static final String CONTAINER = "object-storage-contract";
    private static final String KEY = "sample.json";

    @Autowired
    private ObjectStorageClient storage;

    @Test
    void shouldSupportBlobLifecycleThroughPublicContract() throws Exception {
        byte[] firstContent = "first".getBytes(StandardCharsets.UTF_8);
        UploadResult first = storage.upload(
                new UploadRequest("audit", KEY, firstContent.length,
                        new ObjectHeaders("application/json", "no-cache", "inline", "identity", "en"),
                        Map.of("revision", "first"), WriteMode.CREATE_ONLY),
                new ByteArrayInputStream(firstContent));
        assertNull(first.reference().versionId());
        assertNotNull(first.eTag());
        assertEquals(firstContent.length, first.sizeBytes());
        assertEquals("p5N7ZLjKpY8Dchu2us9ceMsjX+vg5wsbhM2ZVBRhoI4=", first.sha256());

        assertThrows(ConflictException.class, () -> storage.upload(
                new UploadRequest("audit", KEY, firstContent.length, "application/json", Map.of("revision", "duplicate")),
                new ByteArrayInputStream(firstContent)));

        byte[] secondContent = "second".getBytes(StandardCharsets.UTF_8);
        UploadResult second = storage.upload(
                new UploadRequest("audit", KEY, secondContent.length,
                        new ObjectHeaders("application/json", "no-cache", "inline", "identity", "en"),
                        Map.of("revision", "second"), WriteMode.OVERWRITE),
                new ByteArrayInputStream(secondContent));
        assertTrue(storage.exists(ObjectReference.current("audit", KEY)));
        assertEquals("second", storage.head(ObjectReference.current("audit", KEY)).metadata().get("revision"));
        assertEquals(new ObjectHeaders("application/json", "no-cache", "inline", "identity", "en"),
                storage.head(ObjectReference.current("audit", KEY)).headers());
        assertNotNull(storage.head(ObjectReference.current("audit", KEY)).eTag());

        try (DownloadedObject downloaded = storage.download(ObjectReference.current("audit", KEY))) {
            assertEquals("second", new String(downloaded.content().readAllBytes(), StandardCharsets.UTF_8));
        }

        assertEquals(1, storage.list("audit", "sample", null, 10).objects().size());
        storage.delete(ObjectReference.current("audit", KEY));
        assertFalse(storage.exists(ObjectReference.current("audit", KEY)));
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
            long successes = results.stream().filter(this::get).count();
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

    private boolean get(Future<Boolean> result) {
        try {
            return result.get(20, TimeUnit.SECONDS);
        } catch (Exception exception) {
            throw new AssertionError("Concurrent upload did not complete", exception);
        }
    }

    @Configuration(proxyBeanMethods = false)
    @WithAzureEmulator(blobStorage = @AzureBlobStorage(containers = CONTAINER))
    static class AzuriteConfiguration {
        @Bean
        ObjectStorageClient objectStorageClient(BlobServiceClient blobServiceClient) {
            ObjectStorageProperties properties = new ObjectStorageProperties();
            properties.setProvider(ObjectStorageProvider.AZURE);
            properties.setEndpoint("http://127.0.0.1:10000/devstoreaccount1");
            ObjectStorageProperties.Destination destination = new ObjectStorageProperties.Destination();
            destination.setContainer(CONTAINER);
            destination.setPrefix("objects/");
            destination.setSuffix(".data");
            properties.setDestinations(Map.of("audit", destination));
            return new AzureBlobObjectStorageClient(blobServiceClient, properties);
        }
    }
}
