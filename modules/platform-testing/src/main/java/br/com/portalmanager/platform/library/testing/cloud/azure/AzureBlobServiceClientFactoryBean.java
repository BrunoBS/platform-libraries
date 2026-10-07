package br.com.portalmanager.platform.library.testing.cloud.azure;

import com.azure.messaging.servicebus.ServiceBusSenderClient;
import com.azure.storage.blob.BlobServiceClient;
import com.azure.storage.blob.BlobServiceClientBuilder;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.beans.factory.FactoryBean;
import org.springframework.beans.factory.ObjectProvider;

public final class AzureBlobServiceClientFactoryBean implements FactoryBean<BlobServiceClient>, DisposableBean {

    private final AzureBlobStorageContainer container;
    private final ObjectProvider<AzureServiceBusContainer> serviceBusContainers;
    private BlobServiceClient client;
    private AzureBlobCreatedEventPolicy eventPolicy;

    public AzureBlobServiceClientFactoryBean(
            AzureBlobStorageContainer container,
            ObjectProvider<AzureServiceBusContainer> serviceBusContainers) {
        this.container = container;
        this.serviceBusContainers = serviceBusContainers;
    }

    @Override
    public BlobServiceClient getObject() {
        if (client == null) {
            BlobServiceClientBuilder builder = new BlobServiceClientBuilder()
                    .connectionString(container.getConnectionString());

            if (!container.blobCreatedQueue().isBlank()) {
                AzureServiceBusContainer serviceBus = serviceBusContainers.getObject();
                ServiceBusSenderClient sender = serviceBus.sender(container.blobCreatedQueue());
                eventPolicy = new AzureBlobCreatedEventPolicy(sender);
                builder.addPolicy(eventPolicy);
            }

            client = builder.buildClient();
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

    @Override
    public void destroy() {
        if (eventPolicy != null) {
            eventPolicy.close();
        }
    }
}
