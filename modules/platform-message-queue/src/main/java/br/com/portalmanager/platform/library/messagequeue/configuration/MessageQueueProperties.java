package br.com.portalmanager.platform.library.messagequeue.configuration;

import br.com.portalmanager.platform.library.messaging.exception.PlatformConfigurationException;
import br.com.portalmanager.platform.library.messagequeue.message.MessageQueueTechnicalErrors;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.boot.context.properties.ConfigurationProperties;

import java.time.Duration;
import java.util.LinkedHashMap;
import java.util.Map;

@ConfigurationProperties(prefix = "platform.message-queue", ignoreUnknownFields = false)
public class MessageQueueProperties implements InitializingBean {

    private MessageQueueProvider provider;
    private Duration shutdownTimeout = Duration.ofSeconds(30);
    private Duration pollFailureBackoff = Duration.ofSeconds(1);
    private final Aws aws = new Aws();
    private final Azure azure = new Azure();
    private final Map<String, Destination> destinations = new LinkedHashMap<>();

    public MessageQueueProvider getProvider() { return provider; }
    public void setProvider(MessageQueueProvider provider) { this.provider = provider; }
    public Duration getShutdownTimeout() { return shutdownTimeout; }
    public void setShutdownTimeout(Duration shutdownTimeout) { this.shutdownTimeout = shutdownTimeout; }
    public Duration getPollFailureBackoff() { return pollFailureBackoff; }
    public void setPollFailureBackoff(Duration pollFailureBackoff) { this.pollFailureBackoff = pollFailureBackoff; }
    public Aws getAws() { return aws; }
    public Azure getAzure() { return azure; }
    public Map<String, Destination> getDestinations() { return destinations; }

    @Override
    public void afterPropertiesSet() { validate(); }

    void validate() {
        if (provider == null) throw invalid("provider is required");
        if (destinations.isEmpty()) throw invalid("at least one destination is required");
        validateDuration("shutdown-timeout", shutdownTimeout, 0, Long.MAX_VALUE);
        validateDuration("poll-failure-backoff", pollFailureBackoff, 0, Long.MAX_VALUE);

        if (provider == MessageQueueProvider.AWS) {
            if (aws.region == null || aws.region.isBlank()) {
                throw invalid("aws.region is required when provider is AWS");
            }
            validateAwsConsumer("aws.defaults", aws.defaults, aws.defaults.visibilityTimeout);
        } else {
            if (azure.namespace == null || azure.namespace.isBlank()) {
                throw invalid("azure.namespace is required when provider is AZURE");
            }
            validateConsumer("azure.defaults", azure.defaults);
            validateDuration("azure.max-auto-lock-renewal-duration", azure.maxAutoLockRenewalDuration, 0, 43200);
        }
        destinations.forEach(this::validateDestination);
    }

    private void validateDestination(String name, Destination destination) {
        if (name == null || name.isBlank()) throw invalid("destination name must not be blank");
        if (destination == null || destination.queue == null || destination.queue.isBlank()) {
            throw invalid("destination '" + name + "' requires a physical queue");
        }

        validateConsumer(name + ".consumer", destination.consumer);
        if (provider == MessageQueueProvider.AWS) {
            validateAwsConsumer(name + ".aws", destination.aws, destination.aws.visibilityTimeout);
            if (destination.ordered != destination.queue.endsWith(".fifo")) {
                throw invalid(name + ".queue must " + (destination.ordered ?
                        "end with .fifo when ordered is true" : "not end with .fifo when ordered is false"));
            }
            String deadLetterQueue = destination.aws.deadLetterQueue;
            if (deadLetterQueue != null && !deadLetterQueue.isBlank()) {
                if (deadLetterQueue.equals(destination.queue)) {
                    throw invalid("destination '" + name + "' source and dead-letter queues must be different");
                }
                if (destination.ordered != deadLetterQueue.endsWith(".fifo")) {
                    throw invalid(name + ".aws.dead-letter-queue must use the same ordered type as the source queue");
                }
            }
        } else {
            validateConsumer(name + ".azure", destination.azure);
        }
    }

    private void validateConsumer(String name, ConsumerOptions consumer) {
        if (consumer == null) throw invalid(name + " settings are required");
        if (consumer.concurrency != null && consumer.concurrency < 1) {
            throw invalid(name + ".concurrency must be greater than zero");
        }
        if (provider == MessageQueueProvider.AWS) validateDuration(name + ".wait-time", consumer.waitTime, 0, 20);
        else validateDuration(name + ".wait-time", consumer.waitTime, 1, Long.MAX_VALUE);
    }

    private void validateAwsConsumer(String name, ConsumerOptions consumer, Duration visibilityTimeout) {
        validateConsumer(name, consumer);
        validateDuration(name + ".visibility-timeout", visibilityTimeout, 0, 43200);
    }

    private void validateDuration(String name, Duration value, long minSeconds, long maxSeconds) {
        if (value == null) return;
        if (value.isNegative() || value.getNano() != 0) {
            throw invalid(name + " must be a whole number of seconds and non-negative");
        }
        long seconds = value.getSeconds();
        if (seconds < minSeconds || seconds > maxSeconds) {
            throw invalid(name + " must be between " + minSeconds + " and " + maxSeconds + " seconds");
        }
    }

    private PlatformConfigurationException invalid(String reason) {
        return new PlatformConfigurationException(MessageQueueTechnicalErrors.invalidConfiguration(reason));
    }

    public static class Aws {
        private String region;
        private final AwsConsumerOptions defaults = new AwsConsumerOptions();
        public String getRegion() { return region; }
        public void setRegion(String region) { this.region = region; }
        public AwsConsumerOptions getDefaults() { return defaults; }
    }

    public static class Azure {
        private String namespace;
        private Duration maxAutoLockRenewalDuration = Duration.ofMinutes(5);
        private final ConsumerOptions defaults = new ConsumerOptions();
        public String getNamespace() { return namespace; }
        public void setNamespace(String namespace) { this.namespace = namespace; }
        public Duration getMaxAutoLockRenewalDuration() { return maxAutoLockRenewalDuration; }
        public void setMaxAutoLockRenewalDuration(Duration duration) { this.maxAutoLockRenewalDuration = duration; }
        public ConsumerOptions getDefaults() { return defaults; }
    }

    public static class Destination {
        private String queue;
        private boolean ordered;
        private final Toggle publisher = new Toggle();
        private final Consumer consumer = new Consumer();
        private final AwsDestination aws = new AwsDestination();
        private final ConsumerOptions azure = new ConsumerOptions();
        public String getQueue() { return queue; }
        public void setQueue(String queue) { this.queue = queue; }
        public boolean isOrdered() { return ordered; }
        public void setOrdered(boolean ordered) { this.ordered = ordered; }
        public Toggle getPublisher() { return publisher; }
        public Consumer getConsumer() { return consumer; }
        public AwsDestination getAws() { return aws; }
        public ConsumerOptions getAzure() { return azure; }
    }

    public static class Toggle {
        private boolean enabled = true;
        public boolean isEnabled() { return enabled; }
        public void setEnabled(boolean enabled) { this.enabled = enabled; }
    }

    public static class Consumer extends ConsumerOptions {
        private boolean enabled = true;
        public boolean isEnabled() { return enabled; }
        public void setEnabled(boolean enabled) { this.enabled = enabled; }
    }

    public static class ConsumerOptions {
        private Duration waitTime;
        private Integer concurrency;
        public Duration getWaitTime() { return waitTime; }
        public void setWaitTime(Duration waitTime) { this.waitTime = waitTime; }
        public Integer getConcurrency() { return concurrency; }
        public void setConcurrency(Integer concurrency) { this.concurrency = concurrency; }
    }

    public static class AwsConsumerOptions extends ConsumerOptions {
        private Duration visibilityTimeout;
        public Duration getVisibilityTimeout() { return visibilityTimeout; }
        public void setVisibilityTimeout(Duration visibilityTimeout) { this.visibilityTimeout = visibilityTimeout; }
    }

    public static class AwsDestination extends ConsumerOptions {
        private String deadLetterQueue;
        private Duration visibilityTimeout;
        public Duration getVisibilityTimeout() { return visibilityTimeout; }
        public void setVisibilityTimeout(Duration visibilityTimeout) { this.visibilityTimeout = visibilityTimeout; }
        public String getDeadLetterQueue() { return deadLetterQueue; }
        public void setDeadLetterQueue(String deadLetterQueue) { this.deadLetterQueue = deadLetterQueue; }
    }

}
