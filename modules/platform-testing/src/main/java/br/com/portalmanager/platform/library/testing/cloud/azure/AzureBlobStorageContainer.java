package br.com.portalmanager.platform.library.testing.cloud.azure;

import br.com.portalmanager.platform.library.testing.cloud.CloudTestContainer;
import com.azure.storage.blob.BlobServiceClientBuilder;
import org.testcontainers.azure.AzuriteContainer;
import org.testcontainers.utility.DockerImageName;

import java.util.Map;

public final class AzureBlobStorageContainer extends AzuriteContainer implements CloudTestContainer {

    public static final DockerImageName DEFAULT_IMAGE = DockerImageName.parse(
            "mcr.microsoft.com/azure-storage/azurite:3.33.0"
    );

    private final String[] containers;

    public AzureBlobStorageContainer(String[] containers) {
        super(DEFAULT_IMAGE);
        this.containers = containers == null ? new String[0] : containers.clone();
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
            throw new IllegalArgumentException("Azure blob container name must not be blank");
        }
    }

    @Override
    public Map<String, String> connectionProperties() {
        return Map.of("connection-string", getConnectionString());
    }
}
