package br.com.portalmanager.platform.library.objectstorage.provider.azure;

import br.com.portalmanager.platform.library.objectstorage.configuration.ObjectStorageProperties;
import br.com.portalmanager.platform.library.objectstorage.configuration.ObjectStorageProvider;
import br.com.portalmanager.platform.library.objectstorage.contract.DownloadedObject;
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
import java.util.Map;
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
                new UploadRequest("audit", KEY, firstContent.length, "application/json", Map.of("revision", "first")),
                new ByteArrayInputStream(firstContent));
        assertNull(first.reference().versionId());
        assertNotNull(first.eTag());
        assertEquals(firstContent.length, first.sizeBytes());

        assertThrows(ConflictException.class, () -> storage.upload(
                new UploadRequest("audit", KEY, firstContent.length, "application/json", Map.of("revision", "duplicate")),
                new ByteArrayInputStream(firstContent)));

        byte[] secondContent = "second".getBytes(StandardCharsets.UTF_8);
        UploadResult second = storage.upload(
                new UploadRequest("audit", KEY, secondContent.length, "application/json", Map.of("revision", "second"), WriteMode.OVERWRITE),
                new ByteArrayInputStream(secondContent));
        assertTrue(storage.exists(ObjectReference.current("audit", KEY)));
        assertEquals("second", storage.head(ObjectReference.current("audit", KEY)).metadata().get("revision"));
        assertNotNull(storage.head(ObjectReference.current("audit", KEY)).eTag());

        try (DownloadedObject downloaded = storage.download(ObjectReference.current("audit", KEY))) {
            assertEquals("second", new String(downloaded.content().readAllBytes(), StandardCharsets.UTF_8));
        }

        assertEquals(1, storage.list("audit", "sample", null, 10).objects().size());
        storage.delete(ObjectReference.current("audit", KEY));
        assertFalse(storage.exists(ObjectReference.current("audit", KEY)));
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
            properties.setDestinations(Map.of("audit", destination));
            return new AzureBlobObjectStorageClient(blobServiceClient, properties);
        }
    }
}
