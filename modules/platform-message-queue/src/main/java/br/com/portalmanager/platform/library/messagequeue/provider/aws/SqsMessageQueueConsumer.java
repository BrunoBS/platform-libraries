package br.com.portalmanager.platform.library.messagequeue.provider.aws;

import br.com.portalmanager.platform.library.messagequeue.consumer.MessageQueueListenerRegistry;
import br.com.portalmanager.platform.library.messagequeue.exception.MessageConsumeException;
import br.com.portalmanager.platform.library.messagequeue.resolver.DestinationResolver;
import br.com.portalmanager.platform.library.messagequeue.serialization.MessageQueueSerializer;
import org.springframework.context.SmartLifecycle;
import software.amazon.awssdk.services.sqs.SqsClient;
import software.amazon.awssdk.services.sqs.model.DeleteMessageRequest;
import software.amazon.awssdk.services.sqs.model.GetQueueUrlRequest;
import software.amazon.awssdk.services.sqs.model.ReceiveMessageRequest;

import java.time.Duration;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

public class SqsMessageQueueConsumer implements SmartLifecycle {

    private final SqsClient sqsClient;
    private final MessageQueueListenerRegistry registry;
    private final DestinationResolver destinationResolver;
    private final MessageQueueSerializer serializer;
    private final Map<String, String> queueUrls = new ConcurrentHashMap<>();
    private final Map<String, Future<?>> workers = new ConcurrentHashMap<>();
    private final ExecutorService executor = Executors.newVirtualThreadPerTaskExecutor();
    private volatile boolean running;

    public SqsMessageQueueConsumer(
            SqsClient sqsClient,
            MessageQueueListenerRegistry registry,
            DestinationResolver destinationResolver,
            MessageQueueSerializer serializer) {
        this.sqsClient = sqsClient;
        this.registry = registry;
        this.destinationResolver = destinationResolver;
        this.serializer = serializer;
    }

    @Override
    public void start() {
        if (running) {
            return;
        }
        running = true;
        registry.listeners().forEach(listener -> {
            var destination = destinationResolver.resolve(listener.destination());
            if (!destination.consumerEnabled()) {
                return;
            }
            int concurrency = destination.concurrency() == null ? 1 : Math.max(1, destination.concurrency());
            for (int index = 0; index < concurrency; index++) {
                String workerId = listener.destination() + "#" + index;
                workers.put(workerId, executor.submit(() -> poll(listener)));
            }
        });
    }

    private void poll(MessageQueueListenerRegistry.ListenerDefinition listener) {
        var destination = destinationResolver.resolve(listener.destination());
        String queueUrl = queueUrls.computeIfAbsent(destination.queue(), this::resolveQueueUrl);
        int waitTimeSeconds = seconds(destination.waitTime(), 20, 0, 20);
        int visibilityTimeoutSeconds = seconds(destination.visibilityTimeout(), 30, 0, 43200);

        while (running && !Thread.currentThread().isInterrupted()) {
            try {
                var response = sqsClient.receiveMessage(ReceiveMessageRequest.builder()
                        .queueUrl(queueUrl)
                        .maxNumberOfMessages(1)
                        .waitTimeSeconds(waitTimeSeconds)
                        .visibilityTimeout(visibilityTimeoutSeconds)
                        .build());

                response.messages().forEach(message -> process(listener, queueUrl, message.body(), message.receiptHandle()));
            } catch (RuntimeException exception) {
                if (running) {
                    throw new MessageConsumeException("Failed to consume destination: " + listener.destination(), exception);
                }
            }
        }
    }

    private void process(
            MessageQueueListenerRegistry.ListenerDefinition listener,
            String queueUrl,
            String body,
            String receiptHandle) {
        try {
            var message = serializer.deserialize(body, listener.payloadType());
            listener.invoke(message);
            sqsClient.deleteMessage(DeleteMessageRequest.builder()
                    .queueUrl(queueUrl)
                    .receiptHandle(receiptHandle)
                    .build());
        } catch (ReflectiveOperationException exception) {
            throw new MessageConsumeException("Listener invocation failed for destination: " + listener.destination(), exception);
        }
    }

    private String resolveQueueUrl(String queueName) {
        return sqsClient.getQueueUrl(GetQueueUrlRequest.builder()
                        .queueName(queueName)
                        .build())
                .queueUrl();
    }

    private int seconds(Duration value, int defaultValue, int min, int max) {
        if (value == null) {
            return defaultValue;
        }
        long seconds = value.toSeconds();
        if (seconds < min || seconds > max) {
            throw new IllegalArgumentException("Duration seconds must be between " + min + " and " + max);
        }
        return Math.toIntExact(seconds);
    }

    @Override
    public void stop() {
        running = false;
        workers.values().forEach(worker -> worker.cancel(true));
        workers.clear();
        executor.shutdown();
    }

    @Override
    public boolean isRunning() {
        return running;
    }

    @Override
    public boolean isAutoStartup() {
        return true;
    }

    @Override
    public int getPhase() {
        return Integer.MAX_VALUE;
    }
}
