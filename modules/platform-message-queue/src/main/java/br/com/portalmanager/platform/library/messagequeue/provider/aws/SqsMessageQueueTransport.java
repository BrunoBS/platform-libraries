package br.com.portalmanager.platform.library.messagequeue.provider.aws;

import br.com.portalmanager.platform.library.messagequeue.contract.MessageQueuePublishOptions;
import br.com.portalmanager.platform.library.messagequeue.exception.MessagePublishException;
import br.com.portalmanager.platform.library.messagequeue.message.MessageQueueMessageKeys;
import br.com.portalmanager.platform.library.messagequeue.provider.MessageQueueTransport;
import br.com.portalmanager.platform.library.messagequeue.resolver.ResolvedDestination;
import software.amazon.awssdk.services.sqs.SqsClient;
import software.amazon.awssdk.services.sqs.model.GetQueueUrlRequest;
import software.amazon.awssdk.services.sqs.model.SendMessageRequest;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class SqsMessageQueueTransport implements MessageQueueTransport {

    private final SqsClient sqsClient;
    private final Map<String, String> queueUrls = new ConcurrentHashMap<>();

    public SqsMessageQueueTransport(SqsClient sqsClient) {
        this.sqsClient = sqsClient;
    }

    @Override
    public void send(ResolvedDestination destination, String body) {
        send(destination, body, MessageQueuePublishOptions.defaults());
    }

    @Override
    public void send(
            ResolvedDestination destination,
            String body,
            MessageQueuePublishOptions options) {
        try {
            var request = SendMessageRequest.builder()
                    .queueUrl(queueUrls.computeIfAbsent(destination.queue(), this::resolveQueueUrl))
                    .messageBody(body);

            if (destination.ordered()) {
                if (options.orderingKey() == null || options.orderingKey().isBlank()) {
                    throw new IllegalArgumentException("orderingKey is required for an ordered destination");
                }
                validateFifoId("orderingKey", options.orderingKey());
                request.messageGroupId(options.orderingKey());
                if (options.deduplicationId() == null || options.deduplicationId().isBlank()) {
                    throw new IllegalArgumentException(
                            "deduplicationId is required for an AWS SQS FIFO destination");
                }
                validateFifoId("deduplicationId", options.deduplicationId());
                request.messageDeduplicationId(options.deduplicationId());
            } else if (options.orderingKey() != null || options.deduplicationId() != null) {
                throw new IllegalArgumentException("Ordering and deduplication options require an ordered AWS destination");
            }

            sqsClient.sendMessage(request.build());
        } catch (RuntimeException exception) {
            throw new MessagePublishException(
                    MessageQueueMessageKeys.PUBLISH_FAILED,
                    Map.of("0", destination.logicalName()),
                    exception);
        }
    }

    private void validateFifoId(String name, String value) {
        if (value.isBlank() || value.length() > 128) {
            throw new IllegalArgumentException(name + " must contain between 1 and 128 characters");
        }
    }

    private String resolveQueueUrl(String queueName) {
        return sqsClient.getQueueUrl(GetQueueUrlRequest.builder()
                        .queueName(queueName)
                        .build())
                .queueUrl();
    }
}
