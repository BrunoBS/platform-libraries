package br.com.portalmanager.platform.library.messagequeue.provider.aws;

import br.com.portalmanager.platform.library.messagequeue.exception.MessagePublishException;
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
        try {
            String queueUrl = queueUrls.computeIfAbsent(destination.queue(), this::resolveQueueUrl);
            sqsClient.sendMessage(SendMessageRequest.builder()
                    .queueUrl(queueUrl)
                    .messageBody(body)
                    .build());
        } catch (RuntimeException exception) {
            throw new MessagePublishException(
                    "Failed to publish message to destination: " + destination.logicalName(), exception);
        }
    }

    private String resolveQueueUrl(String queueName) {
        return sqsClient.getQueueUrl(GetQueueUrlRequest.builder()
                        .queueName(queueName)
                        .build())
                .queueUrl();
    }
}
