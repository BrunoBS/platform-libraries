package br.com.portalmanager.platform.library.testing.cloud.azure;

import br.com.portalmanager.platform.library.testing.annotation.WithAzureEmulator;
import org.springframework.context.annotation.ImportSelector;
import org.springframework.core.type.AnnotationMetadata;

import java.util.Arrays;
import java.util.Map;

public final class AzureEmulatorImportSelector implements ImportSelector {

    @Override
    public String[] selectImports(AnnotationMetadata importingClassMetadata) {
        Map<String, Object> attributes = importingClassMetadata.getAnnotationAttributes(
                WithAzureEmulator.class.getName(), false);
        if (attributes == null) {
            return new String[0];
        }

        AzureService[] services = (AzureService[]) attributes.get("services");
        return Arrays.stream(services)
                .distinct()
                .map(this::configurationClass)
                .toArray(String[]::new);
    }

    private String configurationClass(AzureService service) {
        return switch (service) {
            case SERVICE_BUS -> AzureServiceBusTestConfiguration.class.getName();
            case BLOB_STORAGE -> AzureBlobStorageTestConfiguration.class.getName();
        };
    }
}
