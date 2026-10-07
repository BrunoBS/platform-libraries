package br.com.portalmanager.platform.library.testing.cloud.azure.blob;

import br.com.portalmanager.platform.library.messaging.exception.PlatformConfigurationException;
import br.com.portalmanager.platform.library.testing.message.PlatformTestingTechnicalErrors;

import com.azure.storage.blob.BlobServiceClientBuilder;
import org.testcontainers.azure.AzuriteContainer;
import org.testcontainers.utility.DockerImageName;


public final class AzureBlobStorageContainer extends AzuriteContainer {

    public static final DockerImageName DEFAULT_IMAGE = DockerImageName.parse(
            "mcr.microsoft.com/azure-storage/azurite:3.37.0"
    );

    private final String[] containers;
    private final String blobCreatedQueue;

    public AzureBlobStorageContainer(String[] containers) {
        this(containers, "");
    }

    public AzureBlobStorageContainer(String[] containers, String blobCreatedQueue) {
        super(DEFAULT_IMAGE);
        this.containers = containers == null ? new String[0] : containers.clone();
        this.blobCreatedQueue = blobCreatedQueue == null ? "" : blobCreatedQueue.trim();
    }

    public String blobCreatedQueue() {
        return blobCreatedQueue;
    }

    @Override
    public void start() {
        super.start();
        try {
            provisionContainers();
        } catch (RuntimeException exception) {
            super.stop();
            throw exception;
        }
    }

    private void provisionContainers() {
        var blobServiceClient = new BlobServiceClientBuilder()
                .connectionString(getConnectionString())
                .buildClient();

        for (String container : containers) {
            requireName(container);
            blobServiceClient.createBlobContainerIfNotExists(container);
        }
    }

    private void requireName(String value) {
        if (value == null || value.isBlank()) {
            throw new PlatformConfigurationException(PlatformTestingTechnicalErrors.invalidAzureConfiguration("Azure blob container name must not be blank"));
        }
    }
}
