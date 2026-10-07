package br.com.portalmanager.platform.library.testing.cloud.azure;

import br.com.portalmanager.platform.library.testing.cloud.azure.annotation.AzureBlobStorage;
import br.com.portalmanager.platform.library.testing.cloud.azure.annotation.AzureServiceBus;
import br.com.portalmanager.platform.library.testing.cloud.azure.annotation.WithAzureEmulator;

import com.azure.messaging.servicebus.ServiceBusClientBuilder;
import com.azure.storage.blob.BlobServiceClient;
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
    void shouldRejectNonPositiveMaxDeliveryCount() {
        DefaultListableBeanFactory registry = new DefaultListableBeanFactory();

        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> registrar.registerBeanDefinitions(
                        AnnotationMetadata.introspect(InvalidAzureCloudTest.class), registry));

        assertEquals("Azure Service Bus maxDeliveryCount must be a positive integer", exception.getMessage());
    }

    @Test
    void shouldRejectBlobCreatedQueueThatIsNotDeclared() {
        DefaultListableBeanFactory registry = new DefaultListableBeanFactory();

        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> registrar.registerBeanDefinitions(
                        AnnotationMetadata.introspect(InvalidBlobNotificationTest.class), registry));

        assertEquals(
                "Azure BlobCreated notification queue must be declared in AzureServiceBus: missing-events",
                exception.getMessage());
    }

    @Test
    void shouldRegisterTypedClientsForConfiguredAzureServices() {
        DefaultListableBeanFactory registry = new DefaultListableBeanFactory();

        registrar.registerBeanDefinitions(AnnotationMetadata.introspect(AzureCloudTest.class), registry);

        assertTrue(registry.containsBeanDefinition("azureServiceBusContainer"));
        assertTrue(registry.containsBeanDefinition("azureBlobStorageContainer"));
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
            serviceBus = @AzureServiceBus(queues = {
                    @AzureServiceBus.Queue(name = "orders"),
                    @AzureServiceBus.Queue(name = "ordered-orders", sessionsEnabled = true)
            }),
            blobStorage = @AzureBlobStorage(containers = "documents")
    )
    private static final class AzureCloudTest {
    }

    @WithAzureEmulator(
            serviceBus = @AzureServiceBus(queues =
                    @AzureServiceBus.Queue(name = "orders", maxDeliveryCount = 0))
    )
    private static final class InvalidAzureCloudTest {
    }
}
