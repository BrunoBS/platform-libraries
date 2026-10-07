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
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

import java.net.URI;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.OffsetDateTime;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

@SpringJUnitConfig(AzureBlobCreatedEventPolicyIntegrationTest.AzureEmulatorConfiguration.class)
class AzureBlobCreatedEventPolicyIntegrationTest {

    private static final String QUEUE = "audit-events";
    private static final String CONTAINER = "audit-files";
    private static final String CONTENT = "audit-123";
    private static final JsonMapper JSON = JsonMapper.builder().build();

    @Autowired
    private BlobServiceClient blobServiceClient;

    @Autowired
    private ServiceBusClientBuilder serviceBusClientBuilder;

    @Test
    void shouldPublishContractCompatibleBlobCreatedAndAllowReadingBlobFromEvent() throws Exception {
        try (ServiceBusReceiverClient receiver = serviceBusClientBuilder.receiver()
                .queueName(QUEUE)
                .buildClient()) {
            String blobName = "audit-123.json";
            blobServiceClient.getBlobContainerClient(CONTAINER)
                    .getBlobClient(blobName)
                    .upload(BinaryData.fromString(CONTENT), true);

            Iterator<ServiceBusReceivedMessage> messages =
                    receiver.receiveMessages(1, Duration.ofSeconds(20)).iterator();
            assertTrue(messages.hasNext(), "Expected a BlobCreated message in Service Bus");
            ServiceBusReceivedMessage message = messages.next();

            JsonNode actual = JSON.readTree(message.getBody().toString()).get(0);
            JsonNode sample = JSON.readTree(getClass().getResourceAsStream(
                    "/azure/blob-created-event-grid-schema.json")).get(0);

            assertEquals(fieldNames(sample), fieldNames(actual));
            assertEquals(fieldNames(sample.path("data")), fieldNames(actual.path("data")));
            assertEquals(fieldNames(sample.path("data").path("storageDiagnostics")),
                    fieldNames(actual.path("data").path("storageDiagnostics")));

            assertEquals("Microsoft.Storage.BlobCreated", actual.path("eventType").asText());
            assertEquals("/blobServices/default/containers/" + CONTAINER + "/blobs/" + blobName,
                    actual.path("subject").asText());
            assertEquals("PutBlob", actual.path("data").path("api").asText());
            assertEquals(CONTENT.length(), actual.path("data").path("contentLength").asInt());
            assertEquals("BlockBlob", actual.path("data").path("blobType").asText());
            assertEquals("Default", actual.path("data").path("accessTier").asText());
            assertTrue(actual.path("data").path("url").asText().endsWith("/" + CONTAINER + "/" + blobName));
            OffsetDateTime.parse(actual.path("eventTime").asText());

            assertEquals("Notification", message.getApplicationProperties().get("aeg-event-type"));
            assertNotNull(message.getApplicationProperties().get("aeg-subscription-name"));
            assertNotNull(message.getApplicationProperties().get("aeg-delivery-count"));
            assertEquals("1", message.getApplicationProperties().get("aeg-metadata-version"));
            assertEquals("", message.getApplicationProperties().get("aeg-data-version"));
            assertEquals(actual.path("id").asText(),
                    message.getApplicationProperties().get("aeg-output-event-id"));

            assertEquals(CONTENT, readBlobReferencedByEvent(actual.path("data").path("url").asText()));
            receiver.complete(message);
        }
    }

    private String readBlobReferencedByEvent(String blobUrl) {
        String[] segments = URI.create(blobUrl).getRawPath().split("/");
        String containerName = decode(segments[2]);
        String blobName = Arrays.stream(segments, 3, segments.length)
                .map(this::decode)
                .collect(Collectors.joining("/"));
        return blobServiceClient.getBlobContainerClient(containerName)
                .getBlobClient(blobName)
                .downloadContent()
                .toString();
    }

    private String decode(String value) {
        return URLDecoder.decode(value.replace("+", "%2B"), StandardCharsets.UTF_8);
    }

    private Set<String> fieldNames(JsonNode node) {
        Set<String> names = new HashSet<>();
        node.fieldNames().forEachRemaining(names::add);
        return names;
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
