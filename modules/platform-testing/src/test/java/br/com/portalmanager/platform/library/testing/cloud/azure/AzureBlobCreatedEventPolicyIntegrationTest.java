package br.com.portalmanager.platform.library.testing.cloud.azure;

import br.com.portalmanager.platform.library.testing.cloud.azure.annotation.AzureBlobStorage;
import br.com.portalmanager.platform.library.testing.cloud.azure.annotation.AzureServiceBus;
import br.com.portalmanager.platform.library.testing.cloud.azure.annotation.WithAzureEmulator;

import com.azure.core.util.BinaryData;
import com.azure.messaging.servicebus.ServiceBusClientBuilder;
import com.azure.messaging.servicebus.ServiceBusReceivedMessage;
import com.azure.messaging.servicebus.ServiceBusReceiverClient;
import com.azure.storage.blob.BlobServiceClient;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Configuration;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;

import java.time.Duration;
import java.util.Iterator;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

@SpringJUnitConfig(AzureBlobCreatedEventPolicyIntegrationTest.AzureEmulatorConfiguration.class)
class AzureBlobCreatedEventPolicyIntegrationTest {

    private static final String QUEUE = "audit-events";
    private static final String CONTAINER = "audit-files";

    @Autowired
    private BlobServiceClient blobServiceClient;

    @Autowired
    private ServiceBusClientBuilder serviceBusClientBuilder;

    @Test
    void shouldPublishBlobCreatedEventAfterUploadCompletes() {
        try (ServiceBusReceiverClient receiver = serviceBusClientBuilder.receiver()
                .queueName(QUEUE)
                .buildClient()) {
            String blobName = "audit-123.json";
            blobServiceClient.getBlobContainerClient(CONTAINER)
                    .getBlobClient(blobName)
                    .upload(BinaryData.fromString("audit-123"), true);

            Iterator<ServiceBusReceivedMessage> messages =
                    receiver.receiveMessages(1, Duration.ofSeconds(20)).iterator();
            assertTrue(messages.hasNext(), "Expected a BlobCreated message in Service Bus");
            ServiceBusReceivedMessage message = messages.next();
            String body = message.getBody().toString();

            assertTrue(body.contains("Microsoft.Storage.BlobCreated"));
            assertTrue(body.contains("/containers/" + CONTAINER + "/blobs/" + blobName));
            assertTrue(body.contains("PutBlob"));
            assertEquals("Notification", message.getApplicationProperties().get("aeg-event-type"));
            assertNotNull(message.getApplicationProperties().get("aeg-output-event-id"));
            receiver.complete(message);
        }
    }

    @Configuration(proxyBeanMethods = false)
    @WithAzureEmulator(
            serviceBus = @AzureServiceBus(queues =
                    @AzureServiceBus.Queue(name = QUEUE)),
            blobStorage = @AzureBlobStorage(
                    containers = CONTAINER,
                    blobCreatedQueue = QUEUE)
    )
    static class AzureEmulatorConfiguration {
    }
}
