package br.com.portalmanager.platform.library.messagequeue.provider.azure;

import br.com.portalmanager.platform.library.messagequeue.MessageQueueContractTestApplication;
import br.com.portalmanager.platform.library.messagequeue.contract.MessageQueuePublisher;
import br.com.portalmanager.platform.library.testing.annotation.AzureServiceBus;
import br.com.portalmanager.platform.library.testing.annotation.WithAzureEmulator;
import com.azure.messaging.servicebus.ServiceBusClientBuilder;
import com.azure.messaging.servicebus.models.ServiceBusReceiveMode;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.TestPropertySource;

import java.time.Duration;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(classes = MessageQueueContractTestApplication.class)
@TestPropertySource(properties = {
        "platform.message-queue.provider=AZURE",
        "spring.application.name=platform-message-queue-test",
        "platform.message-queue.destinations.contract.queue=contract-queue"
})
@WithAzureEmulator(
        serviceBus = @AzureServiceBus(queues = "contract-queue")
)
class AzureServiceBusMessageQueueContractTest {

    @Autowired
    private MessageQueuePublisher publisher;

    @Autowired
    private ServiceBusClientBuilder clientBuilder;

    @Test
    void shouldPublishThroughEmulatorWithoutManualAzureConfiguration() {
        publisher.publish("contract", new ContractPayload("message-1"));

        try (var receiver = clientBuilder.receiver()
                .queueName("contract-queue")
                .receiveMode(ServiceBusReceiveMode.RECEIVE_AND_DELETE)
                .buildClient()) {

            var messages = receiver.receiveMessages(1, Duration.ofSeconds(5));
            var message = messages.stream().findFirst().orElseThrow();

            assertThat(message.getBody().toString())
                    .contains("\"payload\"")
                    .contains("\"id\":\"message-1\"");
        }
    }

    private record ContractPayload(String id) {
    }
}
