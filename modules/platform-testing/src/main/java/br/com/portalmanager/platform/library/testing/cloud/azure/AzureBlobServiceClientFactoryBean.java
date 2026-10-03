package br.com.portalmanager.platform.library.testing.cloud.azure;

import com.azure.storage.blob.BlobServiceClient;
import com.azure.storage.blob.BlobServiceClientBuilder;
import org.springframework.beans.factory.FactoryBean;

public final class AzureBlobServiceClientFactoryBean implements FactoryBean<BlobServiceClient> {

    private final AzureBlobStorageContainer container;
    private BlobServiceClient client;

    public AzureBlobServiceClientFactoryBean(AzureBlobStorageContainer container) {
        this.container = container;
    }

    @Override
    public BlobServiceClient getObject() {
        if (client == null) {
            client = new BlobServiceClientBuilder()
                    .connectionString(container.getConnectionString())
                    .buildClient();
        }
        return client;
    }

    @Override
    public Class<?> getObjectType() {
        return BlobServiceClient.class;
    }

    @Override
    public boolean isSingleton() {
        return true;
    }

    public void close() {
        if (client != null) {
            client.close();
        }
    }
}
