package br.com.portalmanager.platform.library.messagequeue.configuration;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.time.Duration;
import java.util.LinkedHashMap;
import java.util.Map;

@ConfigurationProperties(prefix = "platform.message-queue")
public class MessageQueueProperties {

    private MessageQueueProvider provider;
    private Duration shutdownTimeout = Duration.ofSeconds(30);
    private Duration pollFailureBackoff = Duration.ofSeconds(1);
    private final Aws aws = new Aws();
    private final Azure azure = new Azure();
    private final Map<String, Destination> destinations = new LinkedHashMap<>();

    public MessageQueueProvider getProvider() {
        return provider;
    }

    public void setProvider(MessageQueueProvider provider) {
        this.provider = provider;
    }

    public Duration getShutdownTimeout() {
        return shutdownTimeout;
    }

    public void setShutdownTimeout(Duration shutdownTimeout) {
        this.shutdownTimeout = shutdownTimeout;
    }

    public Duration getPollFailureBackoff() {
        return pollFailureBackoff;
    }

    public void setPollFailureBackoff(Duration pollFailureBackoff) {
        this.pollFailureBackoff = pollFailureBackoff;
    }

    public Aws getAws() {
        return aws;
    }

    public Azure getAzure() {
        return azure;
    }

    public Map<String, Destination> getDestinations() {
        return destinations;
    }

    public static class Aws {
        private String region;
        private final Consumer defaults = new Consumer();

        public String getRegion() {
            return region;
        }

        public void setRegion(String region) {
            this.region = region;
        }

        public Consumer getDefaults() {
            return defaults;
        }
    }

    public static class Azure {
        private String namespace;
        private final Consumer defaults = new Consumer();

        public String getNamespace() {
            return namespace;
        }

        public void setNamespace(String namespace) {
            this.namespace = namespace;
        }

        public Consumer getDefaults() {
            return defaults;
        }
    }

    public static class Destination {
        private String queue;
        private String deadLetterQueue;
        private final Toggle publisher = new Toggle();
        private final Consumer consumer = new Consumer();
        private final Consumer aws = new Consumer();
        private final Consumer azure = new Consumer();

        public String getQueue() {
            return queue;
        }

        public void setQueue(String queue) {
            this.queue = queue;
        }

        public String getDeadLetterQueue() {
            return deadLetterQueue;
        }

        public void setDeadLetterQueue(String deadLetterQueue) {
            this.deadLetterQueue = deadLetterQueue;
        }

        public Toggle getPublisher() {
            return publisher;
        }

        public Consumer getConsumer() {
            return consumer;
        }

        public Consumer getAws() {
            return aws;
        }

        public Consumer getAzure() {
            return azure;
        }
    }

    public static class Toggle {
        private boolean enabled = true;

        public boolean isEnabled() {
            return enabled;
        }

        public void setEnabled(boolean enabled) {
            this.enabled = enabled;
        }
    }

    public static class Consumer extends Toggle {
        private Duration visibilityTimeout;
        private Duration waitTime;
        private Integer concurrency;

        public Duration getVisibilityTimeout() {
            return visibilityTimeout;
        }

        public void setVisibilityTimeout(Duration visibilityTimeout) {
            this.visibilityTimeout = visibilityTimeout;
        }

        public Duration getWaitTime() {
            return waitTime;
        }

        public void setWaitTime(Duration waitTime) {
            this.waitTime = waitTime;
        }

        public Integer getConcurrency() {
            return concurrency;
        }

        public void setConcurrency(Integer concurrency) {
            this.concurrency = concurrency;
        }
    }
}
