package br.com.portalmanager.platform.library.objectstorage.provider.azure;

import br.com.portalmanager.platform.library.objectstorage.configuration.ObjectStorageProperties;
import br.com.portalmanager.platform.library.objectstorage.contract.ObjectHeaders;
import br.com.portalmanager.platform.library.objectstorage.contract.ObjectReference;
import br.com.portalmanager.platform.library.objectstorage.contract.DownloadUrlRequest;
import br.com.portalmanager.platform.library.objectstorage.contract.SignedUrl;
import br.com.portalmanager.platform.library.objectstorage.contract.UploadUrlRequest;
import com.azure.storage.blob.BlobClient;
import com.azure.storage.blob.BlobContainerClient;
import com.azure.storage.blob.BlobServiceClient;
import com.azure.storage.blob.models.UserDelegationKey;
import com.azure.storage.blob.sas.BlobServiceSasSignatureValues;
import java.time.Duration;
import java.util.Map;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.same;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class AzureSignedUrlTest {

    @Test
    void shouldCreateDownloadSasForExactBlobReference() {
        BlobServiceClient serviceClient = mock(BlobServiceClient.class);
        BlobContainerClient containerClient = mock(BlobContainerClient.class);
        BlobClient blobClient = mock(BlobClient.class);
        UserDelegationKey delegationKey = mock(UserDelegationKey.class);
        when(serviceClient.getBlobContainerClient("audit-container")).thenReturn(containerClient);
        when(containerClient.getBlobClient("objects/feature.json")).thenReturn(blobClient);
        when(serviceClient.getUserDelegationKey(any(), any())).thenReturn(delegationKey);
        when(blobClient.getBlobUrl()).thenReturn("https://account.blob.core.windows.net/audit-container/objects/feature.json");
        when(blobClient.generateUserDelegationSas(any(BlobServiceSasSignatureValues.class), same(delegationKey)))
                .thenReturn("sv=2026-01-01&sp=r&sig=signature");

        ObjectStorageProperties properties = new ObjectStorageProperties();
        ObjectStorageProperties.Destination destination = new ObjectStorageProperties.Destination();
        destination.setContainer("audit-container");
        destination.setPrefix("objects");
        destination.setSuffix(".json");
        properties.setDestinations(Map.of("audit", destination));
        AzureBlobObjectStorageClient client = new AzureBlobObjectStorageClient(serviceClient, properties);

        SignedUrl result = client.createDownloadUrl(new DownloadUrlRequest(
                ObjectReference.current("audit", "feature"), Duration.ofMinutes(4)));

        assertTrue(result.url().toString().contains("sp=r"));
        assertTrue(result.requiredHeaders().isEmpty());
        verify(blobClient).generateUserDelegationSas(any(BlobServiceSasSignatureValues.class), same(delegationKey));
    }

    @Test
    void shouldCreateUploadSasForOneBlobWithDelegatedPermissionsAndRequiredHeaders() {
        BlobServiceClient serviceClient = mock(BlobServiceClient.class);
        BlobContainerClient containerClient = mock(BlobContainerClient.class);
        BlobClient blobClient = mock(BlobClient.class);
        UserDelegationKey delegationKey = mock(UserDelegationKey.class);
        when(serviceClient.getBlobContainerClient("audit-container")).thenReturn(containerClient);
        when(containerClient.getBlobClient("objects/feature.json")).thenReturn(blobClient);
        when(serviceClient.getUserDelegationKey(any(), any())).thenReturn(delegationKey);
        when(blobClient.getBlobUrl()).thenReturn("https://account.blob.core.windows.net/audit-container/objects/feature.json");
        when(blobClient.generateUserDelegationSas(any(BlobServiceSasSignatureValues.class), same(delegationKey)))
                .thenReturn("sv=2026-01-01&sp=cw&sig=signature");

        ObjectStorageProperties properties = new ObjectStorageProperties();
        ObjectStorageProperties.Destination destination = new ObjectStorageProperties.Destination();
        destination.setContainer("audit-container");
        destination.setPrefix("objects");
        destination.setSuffix(".json");
        properties.setDestinations(Map.of("audit", destination));
        AzureBlobObjectStorageClient client = new AzureBlobObjectStorageClient(serviceClient, properties);
        ObjectHeaders headers = new ObjectHeaders("application/json", "no-cache", null, null, null);

        SignedUrl result = client.createUploadUrl(new UploadUrlRequest(
                ObjectReference.current("audit", "feature"), Duration.ofMinutes(4), headers));

        assertEquals("https://account.blob.core.windows.net/audit-container/objects/feature.json?sv=2026-01-01&sp=cw&sig=signature",
                result.url().toString());
        assertEquals(Map.of("Content-Type", "application/json", "Cache-Control", "no-cache"), result.requiredHeaders());
        assertTrue(result.expiresAt().isAfter(java.time.Instant.now()));
        verify(serviceClient).getUserDelegationKey(any(), any());
        verify(blobClient).generateUserDelegationSas(any(BlobServiceSasSignatureValues.class), same(delegationKey));
    }
}
