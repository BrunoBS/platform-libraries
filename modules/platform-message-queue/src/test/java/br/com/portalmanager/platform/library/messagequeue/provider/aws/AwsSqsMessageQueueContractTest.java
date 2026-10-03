package br.com.portalmanager.platform.library.messagequeue.provider.aws;

import br.com.portalmanager.platform.library.messagequeue.MessageQueueContractTestApplication;
import br.com.portalmanager.platform.library.messagequeue.contract.MessageQueuePublisher;
import br.com.portalmanager.platform.library.testing.annotation.AwsSqs;
import br.com.portalmanager.platform.library.testing.annotation.WithAwsLocalStack;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.TestPropertySource;
import software.amazon.awssdk.services.sqs.SqsClient;
import software.amazon.awssdk.services.sqs.model.DeleteMessageRequest;
import software.amazon.awssdk.services.sqs.model.GetQueueUrlRequest;
import software.amazon.awssdk.services.sqs.model.ReceiveMessageRequest;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(classes = MessageQueueContractTestApplication.class)
@TestPropertySource(properties = {
        "platform.message-queue.provider=AWS",
        "spring.application.name=platform-message-queue-test",
        "platform.message-queue.destinations.contract.queue=contract-queue"
})
@WithAwsLocalStack(
        sqs = @AwsSqs(queues = "contract-queue")
)
class AwsSqsMessageQueueContractTest {

    @Autowired
    private MessageQueuePublisher publisher;

    @Autowired
    private SqsClient sqsClient;

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

    private record ContractPayload(String id) {
    }
}
