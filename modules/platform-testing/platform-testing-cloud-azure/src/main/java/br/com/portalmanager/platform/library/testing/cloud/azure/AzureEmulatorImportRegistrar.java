package br.com.portalmanager.platform.library.testing.cloud.azure;

import br.com.portalmanager.platform.library.messaging.exception.PlatformConfigurationException;
import br.com.portalmanager.platform.library.testing.cloud.azure.AzureTestingTechnicalErrors;
import br.com.portalmanager.platform.library.testing.container.PinnedDockerImage;

import br.com.portalmanager.platform.library.testing.cloud.azure.blob.AzureBlobStorageContainer;
import br.com.portalmanager.platform.library.testing.cloud.azure.blob.AzureBlobStorageTestSupport;
import br.com.portalmanager.platform.library.testing.cloud.azure.servicebus.AzureServiceBusContainer;
import br.com.portalmanager.platform.library.testing.cloud.azure.servicebus.AzureServiceBusTestSupport;
import br.com.portalmanager.platform.library.testing.cloud.azure.blob.annotation.AzureBlobStorage;
import br.com.portalmanager.platform.library.testing.cloud.azure.servicebus.annotation.AzureServiceBus;
import br.com.portalmanager.platform.library.testing.cloud.azure.annotation.WithAzureEmulator;

import org.springframework.beans.factory.support.BeanDefinitionRegistry;
import org.springframework.beans.factory.support.RootBeanDefinition;
import org.springframework.context.annotation.ImportBeanDefinitionRegistrar;
import org.springframework.core.annotation.AnnotationAttributes;
import org.springframework.core.type.AnnotationMetadata;
import org.testcontainers.utility.DockerImageName;

import java.util.Map;
import java.util.HashSet;
import java.util.Set;

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
        String blobCreatedQueue = blobCreatedQueue(blobStorage, serviceBus);

        if (serviceBus.length == 0 && blobStorage.length == 0) {
            throw configurationException("At least one Azure service annotation must be configured");
        }

        if (serviceBus.length == 1) {
            registerServiceBus(registry, serviceBus[0]);
            SERVICE_BUS_SUPPORT.register(registry);
        }
        if (blobStorage.length == 1) {
            registerBlobStorage(
                    registry,
                    dockerImageName(blobStorage[0].getString("image"), "Azure Blob Storage"),
                    blobStorage[0].getStringArray("containers"),
                    blobCreatedQueue);
            BLOB_STORAGE_SUPPORT.register(registry);
        }
    }

    private void registerServiceBus(BeanDefinitionRegistry registry, AnnotationAttributes serviceBus) {
        AnnotationAttributes[] queues = annotations(serviceBus.get("queues"));
        String configuration = serviceBusConfiguration(queues);
        DockerImageName serviceBusImage = dockerImageName(
                serviceBus.getString("image"), "Azure Service Bus");
        DockerImageName sqlServerImage = dockerImageName(
                serviceBus.getString("sqlServerImage"), "Azure SQL Server");
        RootBeanDefinition definition = new RootBeanDefinition(AzureServiceBusContainer.class);
        definition.getConstructorArgumentValues().addIndexedArgumentValue(0, serviceBusImage);
        definition.getConstructorArgumentValues().addIndexedArgumentValue(1, sqlServerImage);
        definition.getConstructorArgumentValues().addIndexedArgumentValue(2, configuration);
        definition.setInitMethodName("start");
        definition.setDestroyMethodName("stop");
        registry.registerBeanDefinition(SERVICE_BUS_CONTAINER_BEAN, definition);
    }

    private void registerBlobStorage(
            BeanDefinitionRegistry registry,
            DockerImageName image,
            String[] containers,
            String blobCreatedQueue) {
        String[] configuredContainers = containers.clone();
        RootBeanDefinition definition = new RootBeanDefinition(AzureBlobStorageContainer.class);
        definition.getConstructorArgumentValues().addIndexedArgumentValue(0, image);
        definition.getConstructorArgumentValues().addIndexedArgumentValue(1, configuredContainers);
        definition.getConstructorArgumentValues().addIndexedArgumentValue(2, blobCreatedQueue);
        definition.setInitMethodName("start");
        definition.setDestroyMethodName("stop");
        registry.registerBeanDefinition(BLOB_STORAGE_BEAN, definition);
    }

    private String blobCreatedQueue(
            AnnotationAttributes[] blobStorage,
            AnnotationAttributes[] serviceBus) {
        if (blobStorage.length == 0) {
            return "";
        }

        String queueName = blobStorage[0].getString("blobCreatedQueue");
        if (queueName == null || queueName.isBlank()) {
            return "";
        }
        if (serviceBus.length == 0) {
            throw configurationException(
                    "Azure BlobCreated notifications require Azure Service Bus to be configured");
        }

        AnnotationAttributes[] queues = annotations(serviceBus[0].get("queues"));
        for (AnnotationAttributes queue : queues) {
            if (queueName.equals(queue.getString("name"))) {
                return queueName;
            }
        }
        throw configurationException(
                "Azure BlobCreated notification queue must be declared in AzureServiceBus: " + queueName);
    }

    private String serviceBusConfiguration(AnnotationAttributes[] queues) {
        StringBuilder configuredQueues = new StringBuilder();
        Set<String> queueNames = new HashSet<>();
        for (int index = 0; index < queues.length; index++) {
            String name = json(queues[index].getString("name"));
            if (!queueNames.add(name)) {
                throw configurationException("Duplicate Azure Service Bus queue name: " + name);
            }
            int maxDeliveryCount = queues[index].getNumber("maxDeliveryCount").intValue();
            boolean sessionsEnabled = queues[index].getBoolean("sessionsEnabled");
            if (maxDeliveryCount < 1) {
                throw configurationException(
                        "Azure Service Bus maxDeliveryCount must be a positive integer");
            }
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
                        "MaxDeliveryCount": %d,
                        "RequiresDuplicateDetection": false,
                        "RequiresSession": %s
                      }
                    }
                    """.formatted(name, maxDeliveryCount, sessionsEnabled));
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
            throw configurationException("Azure resource name must not be blank");
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
        throw configurationException("Unsupported nested Azure service annotation metadata");
    }

    private DockerImageName dockerImageName(String image, String component) {
        if (image == null || image.isBlank()) {
            throw configurationException(component + " image must not be blank");
        }
        try {
            return DockerImageName.parse(PinnedDockerImage.validatePinnedImage(image));
        } catch (IllegalArgumentException exception) {
            throw configurationException(component + " image must be a valid Docker image name");
        }
    }

    private PlatformConfigurationException configurationException(String detail) {
        return new PlatformConfigurationException(AzureTestingTechnicalErrors.invalidAzureConfiguration(detail));
    }

    private void requireSingle(String annotationName, AnnotationAttributes[] annotations) {
        if (annotations.length > 1) {
            throw configurationException(annotationName + " may be declared only once");
        }
    }
}
