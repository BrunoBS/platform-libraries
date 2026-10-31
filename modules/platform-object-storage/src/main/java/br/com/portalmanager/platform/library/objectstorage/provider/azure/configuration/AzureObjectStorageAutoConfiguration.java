package br.com.portalmanager.platform.library.objectstorage.provider.azure.configuration;

import br.com.portalmanager.platform.library.objectstorage.configuration.ObjectStorageProperties;
import br.com.portalmanager.platform.library.objectstorage.contract.ObjectStorageClient;
import br.com.portalmanager.platform.library.objectstorage.provider.azure.AzureBlobObjectStorageClient;
import com.azure.identity.DefaultAzureCredentialBuilder;
import com.azure.storage.blob.BlobServiceClient;
import com.azure.storage.blob.BlobServiceClientBuilder;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;

@AutoConfiguration
@ConditionalOnProperty(prefix = "platform.object-storage", name = "provider", havingValue = "AZURE")
public class AzureObjectStorageAutoConfiguration {
    @Bean
    @ConditionalOnMissingBean(BlobServiceClient.class)
    BlobServiceClient objectStorageBlobServiceClient(ObjectStorageProperties properties) {
        if (properties.getEndpoint() == null || properties.getEndpoint().isBlank()) {
            throw new IllegalStateException("platform.object-storage.endpoint is required for AZURE");
        }
        return new BlobServiceClientBuilder().endpoint(properties.getEndpoint())
                .credential(new DefaultAzureCredentialBuilder().build()).buildClient();
    }

    @Bean
    @ConditionalOnMissingBean(ObjectStorageClient.class)
    ObjectStorageClient azureObjectStorageClient(BlobServiceClient client, ObjectStorageProperties properties) {
        return new AzureBlobObjectStorageClient(client, properties);
    }
}
