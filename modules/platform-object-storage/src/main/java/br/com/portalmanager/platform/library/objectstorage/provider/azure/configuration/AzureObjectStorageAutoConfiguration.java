package br.com.portalmanager.platform.library.objectstorage.provider.azure.configuration;

import br.com.portalmanager.platform.library.objectstorage.api.configuration.ObjectStorageProperties;
import br.com.portalmanager.platform.library.objectstorage.api.contract.ObjectStorageClient;
import br.com.portalmanager.platform.library.messaging.api.exception.PlatformConfigurationException;
import br.com.portalmanager.platform.library.objectstorage.provider.azure.AzureBlobObjectStorageClient;
import br.com.portalmanager.platform.library.objectstorage.internal.message.ObjectStorageTechnicalErrors;
import com.azure.identity.DefaultAzureCredentialBuilder;
import com.azure.storage.blob.BlobServiceClient;
import com.azure.storage.blob.BlobServiceClientBuilder;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;

@AutoConfiguration
@ConditionalOnProperty(prefix = "platform.object-storage", name = "provider", havingValue = "AZURE")
public class AzureObjectStorageAutoConfiguration {
    @Bean
    @ConditionalOnMissingBean(BlobServiceClient.class)
    BlobServiceClient objectStorageBlobServiceClient(ObjectStorageProperties properties) {
        try {
            return new BlobServiceClientBuilder().endpoint(properties.getEndpoint())
                    .credential(new DefaultAzureCredentialBuilder().build()).buildClient();
        } catch (RuntimeException exception) {
            throw new PlatformConfigurationException(
                    ObjectStorageTechnicalErrors.invalidConfiguration("Azure Blob endpoint is invalid"), exception);
        }
    }

    @Bean
    @ConditionalOnMissingBean(ObjectStorageClient.class)
    AzureBlobObjectStorageClient azureObjectStorageClient(BlobServiceClient client, ObjectStorageProperties properties) {
        return new AzureBlobObjectStorageClient(client, properties);
    }
}
