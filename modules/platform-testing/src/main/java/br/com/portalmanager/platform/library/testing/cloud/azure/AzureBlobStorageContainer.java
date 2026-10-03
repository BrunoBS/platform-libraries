package br.com.portalmanager.platform.library.testing.cloud.azure;

import br.com.portalmanager.platform.library.testing.cloud.CloudTestContainer;
import org.testcontainers.azure.AzuriteContainer;
import org.testcontainers.utility.DockerImageName;

import java.util.Map;

public final class AzureBlobStorageContainer extends AzuriteContainer implements CloudTestContainer {

    public static final DockerImageName DEFAULT_IMAGE = DockerImageName.parse(
            "mcr.microsoft.com/azure-storage/azurite:3.33.0"
    );

    public AzureBlobStorageContainer() {
        super(DEFAULT_IMAGE);
    }

    @Override
    public Map<String, String> connectionProperties() {
        return Map.of("connection-string", getConnectionString());
    }
}
