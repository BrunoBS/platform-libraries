package br.com.portalmanager.platform.library.messagequeue.provider.aws;

import br.com.portalmanager.platform.library.messagequeue.MessageQueueContractListeners;
import br.com.portalmanager.platform.library.messagequeue.MessageQueueContractTestApplication;
import br.com.portalmanager.platform.library.messagequeue.contract.MessageQueuePublisher;
import br.com.portalmanager.platform.library.testing.annotation.AwsSqs;
import br.com.portalmanager.platform.library.testing.annotation.WithAwsLocalStack;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.TestPropertySource;
import software.amazon.awssdk.services.sqs.SqsClient;
import software.amazon.awssdk.services.sqs.model.DeleteMessageRequest;
import software.amazon.awssdk.services.sqs.model.GetQueueUrlRequest;
import software.amazon.awssdk.services.sqs.model.ReceiveMessageRequest;

import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(classes = MessageQueueContractTestApplication.class)
@TestPropertySource(properties = {
        "platform.message-queue.provider=AWS",
        "platform.message-queue.aws.region=sa-east-1",
        "spring.application.name=platform-message-queue-test",
        "platform.message-queue.destinations.contract.queue=contract-queue",
        "platform.message-queue.destinations.consumer-contract.queue=consumer-contract",
        "platform.message-queue.destinations.failing-contract.queue=failing-contract",
        "platform.message-queue.destinations.failing-contract.aws.visibility-timeout=PT1S"
})
@WithAwsLocalStack(
        sqs = @AwsSqs(queues = {
                @AwsSqs.Queue(name = "contract-queue"),
                @AwsSqs.Queue(name = "consumer-contract"),
                @AwsSqs.Queue(
                        name = "failing-contract",
                        deadLetterEnabled = true,
                        maxReceiveCount = 2)
        })
)
class AwsSqsMessageQueueContractTest {

    @Autowired
    private MessageQueuePublisher publisher;

    @Autowired
    private SqsClient sqsClient;

    @BeforeEach
    void resetListenerState() {
        MessageQueueContractListeners.reset();
    }

    @Test
    void shouldPublishThroughLocalStackWithoutManualAwsConfiguration() {
        publisher.publish("contract", new ContractPayload("message-1"));

        String queueUrl = sqsClient.getQueueUrl(GetQueueUrlRequest.builder()
                .queueName("contract-queue")
                .build()).queueUrl();

        var messages = sqsClient.receiveMessage(ReceiveMessageRequest.builder()
                .queueUrl(queueUrl)
                .maxNumberOfMessages(1)
                .waitTimeSeconds(5)
                .build()).messages();

        assertThat(messages).hasSize(1);
        assertThat(messages.getFirst().body())
                .contains("\"payload\"")
                .contains("\"id\":\"message-1\"");

        sqsClient.deleteMessage(DeleteMessageRequest.builder()
                .queueUrl(queueUrl)
                .receiptHandle(messages.getFirst().receiptHandle())
                .build());
    }

    @Test
    void shouldConsumeAndDeleteMessageAfterListenerSucceeds() throws InterruptedException {
        publisher.publish("consumer-contract", new MessageQueueContractListeners.ContractPayload("consume-1"));

        var message = MessageQueueContractListeners.RECEIVED.poll(20, TimeUnit.SECONDS);

        assertThat(message).isNotNull();
        assertThat(message.messageType()).isEqualTo("consumer-contract");
        assertThat(message.payload().id()).isEqualTo("consume-1");
    }

    @Test
    void shouldMoveRepeatedlyFailedMessageToConfiguredDeadLetterQueue() throws InterruptedException {
        publisher.publish("failing-contract", new MessageQueueContractListeners.ContractPayload("fail-1"));

        var deadLetter = MessageQueueContractListeners.DEAD_LETTERED.poll(20, TimeUnit.SECONDS);

        assertThat(deadLetter).isNotNull();
        assertThat(deadLetter.message().payload().id()).isEqualTo("fail-1");
        assertThat(MessageQueueContractListeners.FAILED_DELIVERIES.get()).isGreaterThanOrEqualTo(2);
        assertThat(deadLetter.providerMetadata()).containsEntry("provider", "AWS");
    }

    private record ContractPayload(String id) {
    }
}
