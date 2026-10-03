package br.com.portalmanager.platform.library.testing.cloud.azure;

import br.com.portalmanager.platform.library.testing.annotation.WithAzureEmulator;
import org.springframework.beans.factory.support.BeanDefinitionRegistry;
import org.springframework.beans.factory.support.RootBeanDefinition;
import org.springframework.context.annotation.ImportBeanDefinitionRegistrar;
import org.springframework.core.annotation.AnnotationAttributes;
import org.springframework.core.type.AnnotationMetadata;

import java.util.Map;

public final class AzureEmulatorImportRegistrar implements ImportBeanDefinitionRegistrar {

    private static final String SERVICE_BUS_CONTAINER_BEAN = "azureServiceBusContainer";
    private static final String BLOB_STORAGE_BEAN = "azureBlobStorageContainer";
    private static final AzureServiceTestSupport SERVICE_BUS_SUPPORT = new AzureServiceBusTestSupport();
    private static final AzureBlobStorageTestSupport BLOB_STORAGE_SUPPORT = new AzureBlobStorageTestSupport();

    @Override
    public void registerBeanDefinitions(AnnotationMetadata importingClassMetadata, BeanDefinitionRegistry registry) {
        Map<String, Object> attributes = importingClassMetadata.getAnnotationAttributes(
                WithAzureEmulator.class.getName(), false);
        if (attributes == null) {
            return;
        }

        AnnotationAttributes[] serviceBus = annotations(attributes.get("serviceBus"));
        AnnotationAttributes[] blobStorage = annotations(attributes.get("blobStorage"));
        requireSingle("AzureServiceBus", serviceBus);
        requireSingle("AzureBlobStorage", blobStorage);

        if (serviceBus.length == 0 && blobStorage.length == 0) {
            throw new IllegalArgumentException("At least one Azure service annotation must be configured");
        }

        if (serviceBus.length == 1) {
            registerServiceBus(registry, serviceBus[0].getStringArray("queues"));
            SERVICE_BUS_SUPPORT.register(registry);
        }
        if (blobStorage.length == 1) {
            registerBlobStorage(registry, blobStorage[0].getStringArray("containers"));
            BLOB_STORAGE_SUPPORT.register(registry);
        }
    }

    private void registerServiceBus(BeanDefinitionRegistry registry, String[] queues) {
        String configuration = serviceBusConfiguration(queues);
        RootBeanDefinition definition = new RootBeanDefinition(AzureServiceBusContainer.class);
        definition.setInstanceSupplier(() -> new AzureServiceBusContainer(configuration));
        definition.setInitMethodName("start");
        definition.setDestroyMethodName("stop");
        registry.registerBeanDefinition(SERVICE_BUS_CONTAINER_BEAN, definition);
    }

    private void registerBlobStorage(BeanDefinitionRegistry registry, String[] containers) {
        String[] configuredContainers = containers.clone();
        RootBeanDefinition definition = new RootBeanDefinition(AzureBlobStorageContainer.class);
        definition.setInstanceSupplier(() -> new AzureBlobStorageContainer(configuredContainers));
        definition.setInitMethodName("start");
        definition.setDestroyMethodName("stop");
        registry.registerBeanDefinition(BLOB_STORAGE_BEAN, definition);
    }

    private String serviceBusConfiguration(String[] queues) {
        StringBuilder configuredQueues = new StringBuilder();
        for (int index = 0; index < queues.length; index++) {
            if (index > 0) {
                configuredQueues.append(',');
            }
            configuredQueues.append("""
                    {
                      "Name": "%s",
                      "Properties": {
                        "DeadLetteringOnMessageExpiration": false,
                        "DefaultMessageTimeToLive": "PT1H",
                        "DuplicateDetectionHistoryTimeWindow": "PT20S",
                        "ForwardDeadLetteredMessagesTo": "",
                        "ForwardTo": "",
                        "LockDuration": "PT1M",
                        "MaxDeliveryCount": 3,
                        "RequiresDuplicateDetection": false,
                        "RequiresSession": false
                      }
                    }
                    """.formatted(json(queues[index])));
        }

        return """
                {
                  "UserConfig": {
                    "Namespaces": [
                      {
                        "Name": "sbemulatorns",
                        "Queues": [%s],
                        "Topics": []
                      }
                    ],
                    "Logging": {
                      "Type": "File"
                    }
                  }
                }
                """.formatted(configuredQueues);
    }

    private String json(String value) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException("Azure resource name must not be blank");
        }
        return value.replace("\\", "\\\\").replace("\"", "\\\"");
    }

    @SuppressWarnings("unchecked")
    private AnnotationAttributes[] annotations(Object value) {
        if (value == null) {
            return new AnnotationAttributes[0];
        }
        if (value instanceof AnnotationAttributes[] annotationAttributes) {
            return annotationAttributes;
        }
        if (value instanceof Map<?, ?>[] maps) {
            AnnotationAttributes[] result = new AnnotationAttributes[maps.length];
            for (int index = 0; index < maps.length; index++) {
                result[index] = AnnotationAttributes.fromMap((Map<String, Object>) maps[index]);
            }
            return result;
        }
        throw new IllegalArgumentException("Unsupported nested Azure service annotation metadata");
    }

    private void requireSingle(String annotationName, AnnotationAttributes[] annotations) {
        if (annotations.length > 1) {
            throw new IllegalArgumentException(annotationName + " may be declared only once");
        }
    }
}
