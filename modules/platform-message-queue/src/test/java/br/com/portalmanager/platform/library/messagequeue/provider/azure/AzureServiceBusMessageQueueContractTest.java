package br.com.portalmanager.platform.library.messagequeue.provider.azure;

import br.com.portalmanager.platform.library.messagequeue.MessageQueueContractListeners;
import br.com.portalmanager.platform.library.messagequeue.MessageQueueContractTestApplication;
import br.com.portalmanager.platform.library.messagequeue.contract.MessageQueuePublisher;
import br.com.portalmanager.platform.library.messagequeue.contract.MessageQueuePublishOptions;
import br.com.portalmanager.platform.library.messagequeue.exception.MessagePublishException;
import br.com.portalmanager.platform.library.testing.annotation.AzureServiceBus;
import br.com.portalmanager.platform.library.testing.annotation.WithAzureEmulator;
import com.azure.messaging.servicebus.ServiceBusClientBuilder;
import com.azure.messaging.servicebus.models.ServiceBusReceiveMode;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.TestPropertySource;

import java.time.Duration;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest(classes = MessageQueueContractTestApplication.class)
@TestPropertySource(properties = {
        "platform.message-queue.provider=AZURE",
        "platform.message-queue.azure.namespace=sbemulatorns",
        "spring.application.name=platform-message-queue-test",
        "platform.message-queue.destinations.contract.queue=contract-queue",
        "platform.message-queue.destinations.consumer-contract.queue=consumer-contract",
        "platform.message-queue.destinations.ordered-consumer-contract.queue=ordered-consumer-contract",
        "platform.message-queue.destinations.ordered-publish-contract.queue=ordered-publish-contract",
        "platform.message-queue.destinations.failing-contract.queue=failing-contract",
        "platform.message-queue.destinations.failing-contract.azure.wait-time=PT1S"
})
@WithAzureEmulator(
        serviceBus = @AzureServiceBus(queues = {
                @AzureServiceBus.Queue(name = "contract-queue"),
                @AzureServiceBus.Queue(name = "consumer-contract"),
                @AzureServiceBus.Queue(name = "ordered-consumer-contract", sessionsEnabled = true),
                @AzureServiceBus.Queue(name = "ordered-publish-contract", sessionsEnabled = true),
                @AzureServiceBus.Queue(name = "failing-contract", maxDeliveryCount = 3)
        })
)
class AzureServiceBusMessageQueueContractTest {

    @Autowired
    private MessageQueuePublisher publisher;

    @Autowired
    private ServiceBusClientBuilder clientBuilder;

    @BeforeEach
    void resetListenerState() {
        MessageQueueContractListeners.reset();
    }

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

    @Test
    void shouldConsumeAndCompleteMessageAfterListenerSucceeds() throws InterruptedException {
        publisher.publish("consumer-contract", new MessageQueueContractListeners.ContractPayload("consume-1"));

        var message = MessageQueueContractListeners.RECEIVED.poll(20, TimeUnit.SECONDS);

        assertThat(message).isNotNull();
        assertThat(message.messageType()).isEqualTo("consumer-contract");
        assertThat(message.payload().id()).isEqualTo("consume-1");
    }

    @Test
    void shouldPublishAndConsumeOrderedMessagesUsingTheSessionId() throws InterruptedException {
        publisher.publish("ordered-consumer-contract",
                new MessageQueueContractListeners.ContractPayload("ordered-1"),
                new MessageQueuePublishOptions(null, java.util.Map.of(), "customer-42", null));

        var received = MessageQueueContractListeners.RECEIVED.poll(20, TimeUnit.SECONDS);

        assertThat(received).isNotNull();
        assertThat(received.payload().id()).isEqualTo("ordered-1");
    }

    @Test
    void shouldSetTheConfiguredOrderingKeyAsServiceBusSessionId() {
        publisher.publish("ordered-publish-contract",
                new MessageQueueContractListeners.ContractPayload("session-1"),
                new MessageQueuePublishOptions(null, java.util.Map.of(), "customer-99", null));

        try (var sessionReceiver = clientBuilder.sessionReceiver()
                .queueName("ordered-publish-contract")
                .receiveMode(ServiceBusReceiveMode.RECEIVE_AND_DELETE)
                .buildClient();
             var receiver = sessionReceiver.acceptSession("customer-99")) {
            var message = receiver.receiveMessages(1, Duration.ofSeconds(5)).stream().findFirst().orElseThrow();
            assertThat(message.getSessionId()).isEqualTo("customer-99");
        }
    }

    @Test
    void shouldRequireOrderingKeyForSessionEnabledDestination() {
        assertThatThrownBy(() -> publisher.publish("ordered-publish-contract",
                new MessageQueueContractListeners.ContractPayload("missing-key")))
                .isInstanceOf(MessagePublishException.class)
                .hasRootCauseMessage("orderingKey is required for an ordered destination");
    }

    @Test
    void shouldRejectOrderingKeyForStandardDestination() {
        assertThatThrownBy(() -> publisher.publish("contract",
                new MessageQueueContractListeners.ContractPayload("unexpected-key"),
                new MessageQueuePublishOptions(null, java.util.Map.of(), "customer-42", null)))
                .isInstanceOf(MessagePublishException.class)
                .hasRootCauseMessage("orderingKey can only be used with an ordered destination");
    }

    @Test
    void shouldKeepDeduplicationProviderSpecific() {
        assertThatThrownBy(() -> publisher.publish("ordered-publish-contract",
                new MessageQueueContractListeners.ContractPayload("dedup"),
                new MessageQueuePublishOptions(null, java.util.Map.of(), "customer-42", "dedup-1")))
                .isInstanceOf(MessagePublishException.class)
                .hasRootCauseMessage(
                        "deduplicationId is an AWS SQS FIFO option and is not supported by Azure Service Bus");
    }

    @Test
    void shouldMoveRepeatedlyFailedMessageToAzureDeadLetterSubqueue() throws InterruptedException {
        publisher.publish("failing-contract", new MessageQueueContractListeners.ContractPayload("fail-1"));

        var deadLetter = MessageQueueContractListeners.DEAD_LETTERED.poll(20, TimeUnit.SECONDS);

        assertThat(deadLetter).isNotNull();
        assertThat(deadLetter.message().payload().id()).isEqualTo("fail-1");
        assertThat(MessageQueueContractListeners.FAILED_DELIVERIES.get()).isGreaterThanOrEqualTo(3);
        assertThat(deadLetter.providerMetadata()).containsEntry("provider", "AZURE");
        assertThat(deadLetter.reason()).isNotBlank();
    }

    private record ContractPayload(String id) {
    }
}
