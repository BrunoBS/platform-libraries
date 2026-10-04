package br.com.portalmanager.platform.library.messagequeue.provider.aws;

import br.com.portalmanager.platform.library.messagequeue.capability.QueueCapabilities;
import br.com.portalmanager.platform.library.messagequeue.capability.QueueCapabilitiesResolver;
import software.amazon.awssdk.services.sqs.SqsClient;
import software.amazon.awssdk.services.sqs.model.GetQueueAttributesRequest;
import software.amazon.awssdk.services.sqs.model.GetQueueUrlRequest;
import software.amazon.awssdk.services.sqs.model.QueueAttributeName;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class AwsQueueCapabilitiesResolver implements QueueCapabilitiesResolver {

    private final SqsClient sqsClient;
    private final Map<String, String> queueUrls = new ConcurrentHashMap<>();

    public AwsQueueCapabilitiesResolver(SqsClient sqsClient) {
        this.sqsClient = sqsClient;
    }

    @Override
    public QueueCapabilities resolve(String queue) {
        String queueUrl = queueUrls.computeIfAbsent(queue, this::resolveQueueUrl);
        var response = sqsClient.getQueueAttributes(GetQueueAttributesRequest.builder()
                .queueUrl(queueUrl)
                .attributeNames(
                        QueueAttributeName.FIFO_QUEUE,
                        QueueAttributeName.CONTENT_BASED_DEDUPLICATION)
                .build());

        boolean fifo = Boolean.parseBoolean(response.attributes().getOrDefault(QueueAttributeName.FIFO_QUEUE, "false"));
        boolean contentBasedDeduplication = Boolean.parseBoolean(
                response.attributes().getOrDefault(QueueAttributeName.CONTENT_BASED_DEDUPLICATION, "false"));

        return new QueueCapabilities(fifo, fifo && !contentBasedDeduplication);
    }

    private String resolveQueueUrl(String queueName) {
        return sqsClient.getQueueUrl(GetQueueUrlRequest.builder()
                        .queueName(queueName)
                        .build())
                .queueUrl();
    }
}
