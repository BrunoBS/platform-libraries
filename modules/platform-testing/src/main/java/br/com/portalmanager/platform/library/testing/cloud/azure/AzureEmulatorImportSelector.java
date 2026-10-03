package br.com.portalmanager.platform.library.testing.cloud.azure;

import br.com.portalmanager.platform.library.testing.annotation.WithAzureEmulator;
import org.springframework.context.annotation.ImportSelector;
import org.springframework.core.type.AnnotationMetadata;

import java.util.Arrays;
import java.util.Map;
import java.util.Set;

public final class AzureEmulatorImportSelector implements ImportSelector {

    @Override
    public String[] selectImports(AnnotationMetadata importingClassMetadata) {
        Map<String, Object> attributes = importingClassMetadata.getAnnotationAttributes(
                WithAzureEmulator.class.getName(), false);
        if (attributes == null) {
            return new String[0];
        }

        AzureService[] services = (AzureService[]) attributes.get("services");
        String[] queues = (String[]) attributes.get("queues");
        String[] containers = (String[]) attributes.get("containers");

        validateTopology(services, queues, containers);

        return Arrays.stream(services)
                .distinct()
                .map(this::configurationClass)
                .toArray(String[]::new);
    }

    private void validateTopology(AzureService[] services, String[] queues, String[] containers) {
        if (services == null || services.length == 0) {
            throw new IllegalArgumentException("At least one Azure service must be configured");
        }

        Set<AzureService> enabled = Set.copyOf(Arrays.asList(services));
        if (queues != null && queues.length > 0 && !enabled.contains(AzureService.SERVICE_BUS)) {
            throw new IllegalArgumentException("Azure queues require AzureService.SERVICE_BUS to be enabled");
        }
        if (containers != null && containers.length > 0 && !enabled.contains(AzureService.BLOB_STORAGE)) {
            throw new IllegalArgumentException("Azure containers require AzureService.BLOB_STORAGE to be enabled");
        }
    }

    private String configurationClass(AzureService service) {
        return switch (service) {
            case SERVICE_BUS -> AzureServiceBusTestConfiguration.class.getName();
            case BLOB_STORAGE -> AzureBlobStorageTestConfiguration.class.getName();
        };
    }
}
