package br.com.portalmanager.platform.library.testing.cloud.azure;

import br.com.portalmanager.platform.library.messaging.exception.PlatformConfigurationException;
import br.com.portalmanager.platform.library.testing.cloud.azure.blob.annotation.AzureBlobStorage;
import br.com.portalmanager.platform.library.testing.cloud.azure.servicebus.annotation.AzureServiceBus;
import br.com.portalmanager.platform.library.testing.cloud.azure.annotation.WithAzureEmulator;

import com.azure.messaging.servicebus.ServiceBusClientBuilder;
import com.azure.storage.blob.BlobServiceClient;
import org.springframework.beans.factory.config.ConstructorArgumentValues;
import org.testcontainers.utility.DockerImageName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.FactoryBean;
import org.springframework.beans.factory.support.DefaultListableBeanFactory;
import org.springframework.core.type.AnnotationMetadata;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AzureEmulatorImportRegistrarTest {

    private final AzureEmulatorImportRegistrar registrar = new AzureEmulatorImportRegistrar();

    @Test
    void shouldRejectUnversionedImageWithPlatformConfigurationError() {
        DefaultListableBeanFactory registry = new DefaultListableBeanFactory();

        PlatformConfigurationException exception = assertThrows(
                PlatformConfigurationException.class,
                () -> registrar.registerBeanDefinitions(
                        AnnotationMetadata.introspect(UnversionedAzuriteTest.class), registry));

        assertEquals("PLT-TST-002", exception.getErrorResponse().code());
    }

    @Test
    void shouldRejectNonPositiveMaxDeliveryCount() {
        DefaultListableBeanFactory registry = new DefaultListableBeanFactory();

        PlatformConfigurationException exception = assertThrows(
                PlatformConfigurationException.class,
                () -> registrar.registerBeanDefinitions(
                        AnnotationMetadata.introspect(InvalidAzureCloudTest.class), registry));

        assertEquals("PLT-TST-002", exception.getErrorResponse().code());
        assertEquals("Invalid platform-testing Azure configuration: Azure Service Bus maxDeliveryCount must be a positive integer",
                exception.getErrorResponse().message());
    }

    @Test
    void shouldRejectBlobCreatedQueueThatIsNotDeclared() {
        DefaultListableBeanFactory registry = new DefaultListableBeanFactory();

        PlatformConfigurationException exception = assertThrows(
                PlatformConfigurationException.class,
                () -> registrar.registerBeanDefinitions(
                        AnnotationMetadata.introspect(InvalidBlobNotificationTest.class), registry));

        assertEquals("PLT-TST-002", exception.getErrorResponse().code());
        assertEquals(
                "Invalid platform-testing Azure configuration: Azure BlobCreated notification queue must be declared in AzureServiceBus: missing-events",
                exception.getErrorResponse().message());
    }

    @Test
    void shouldRegisterTypedClientsForConfiguredAzureServices() {
        DefaultListableBeanFactory registry = new DefaultListableBeanFactory();

        registrar.registerBeanDefinitions(AnnotationMetadata.introspect(AzureCloudTest.class), registry);

        assertTrue(registry.containsBeanDefinition("azureServiceBusContainer"));
        assertTrue(registry.containsBeanDefinition("azureBlobStorageContainer"));
        ConstructorArgumentValues serviceBusArguments = registry.getBeanDefinition("azureServiceBusContainer")
                .getConstructorArgumentValues();
        assertEquals(DockerImageName.parse("mcr.microsoft.com/azure-messaging/servicebus-emulator:1.1.3").toString(),
                serviceBusArguments.getIndexedArgumentValue(0, DockerImageName.class).getValue().toString());
        assertEquals(DockerImageName.parse("mcr.microsoft.com/mssql/server:2022-CU15-ubuntu-22.04").toString(),
                serviceBusArguments.getIndexedArgumentValue(1, DockerImageName.class).getValue().toString());
        ConstructorArgumentValues blobArguments = registry.getBeanDefinition("azureBlobStorageContainer")
                .getConstructorArgumentValues();
        assertEquals(DockerImageName.parse("mcr.microsoft.com/azure-storage/azurite:3.38.0").toString(),
                blobArguments.getIndexedArgumentValue(0, DockerImageName.class).getValue().toString());
        assertTrue(registry.containsBeanDefinition("azureServiceBusClientBuilder"));
        assertTrue(registry.containsBeanDefinition("azureBlobServiceClient"));
        assertEquals(ServiceBusClientBuilder.class,
                registry.getBeanDefinition("azureServiceBusClientBuilder")
                        .getAttribute(FactoryBean.OBJECT_TYPE_ATTRIBUTE));
        assertEquals(BlobServiceClient.class,
                registry.getBeanDefinition("azureBlobServiceClient")
                        .getAttribute(FactoryBean.OBJECT_TYPE_ATTRIBUTE));
    }

    @WithAzureEmulator(
            serviceBus = @AzureServiceBus(queues =
                    @AzureServiceBus.Queue(name = "audit-events")),
            blobStorage = @AzureBlobStorage(
                    containers = "audit-files",
                    blobCreatedQueue = "missing-events")
    )
    private static final class InvalidBlobNotificationTest {
    }

    @WithAzureEmulator(
            serviceBus = @AzureServiceBus(
                    image = "mcr.microsoft.com/azure-messaging/servicebus-emulator:1.1.3",
                    sqlServerImage = "mcr.microsoft.com/mssql/server:2022-CU15-ubuntu-22.04",
                    queues = {
                    @AzureServiceBus.Queue(name = "orders"),
                    @AzureServiceBus.Queue(name = "ordered-orders", sessionsEnabled = true)
            }),
            blobStorage = @AzureBlobStorage(
                    image = "mcr.microsoft.com/azure-storage/azurite:3.38.0",
                    containers = "documents")
    )
    private static final class AzureCloudTest {
    }

    @WithAzureEmulator(
            blobStorage = @AzureBlobStorage(
                    image = "mcr.microsoft.com/azure-storage/azurite",
                    containers = "documents")
    )
    private static final class UnversionedAzuriteTest {
    }

    @WithAzureEmulator(
            serviceBus = @AzureServiceBus(queues =
                    @AzureServiceBus.Queue(name = "orders", maxDeliveryCount = 0))
    )
    private static final class InvalidAzureCloudTest {
    }
}
