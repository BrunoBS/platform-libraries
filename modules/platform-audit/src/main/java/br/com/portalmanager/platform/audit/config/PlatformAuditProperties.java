package br.com.portalmanager.platform.audit.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.time.Duration;

@ConfigurationProperties(prefix = "platform.audit")
public class PlatformAuditProperties {

    private boolean enabled = true;
    private String serviceUrl;
    private String publishPath = "/api/v1/events";
    private String serviceName = "unknown";
    private boolean failOnError = false;
    private int corePoolSize = 2;
    private int maxPoolSize = 4;
    private int queueCapacity = 500;
    private final Http http = new Http();
    private final Fallback fallback = new Fallback();

    public boolean isEnabled() { return enabled; }
    public void setEnabled(boolean enabled) { this.enabled = enabled; }

    public String getServiceUrl() { return serviceUrl; }
    public void setServiceUrl(String serviceUrl) { this.serviceUrl = serviceUrl; }

    public String getPublishPath() { return publishPath; }
    public void setPublishPath(String publishPath) { this.publishPath = publishPath; }

    public String getServiceName() { return serviceName; }
    public void setServiceName(String serviceName) { this.serviceName = serviceName; }

    public boolean isFailOnError() { return failOnError; }
    public void setFailOnError(boolean failOnError) { this.failOnError = failOnError; }

    public int getCorePoolSize() { return corePoolSize; }
    public void setCorePoolSize(int corePoolSize) { this.corePoolSize = corePoolSize; }

    public int getMaxPoolSize() { return maxPoolSize; }
    public void setMaxPoolSize(int maxPoolSize) { this.maxPoolSize = maxPoolSize; }

    public int getQueueCapacity() { return queueCapacity; }
    public void setQueueCapacity(int queueCapacity) { this.queueCapacity = queueCapacity; }

    public Http getHttp() { return http; }
    public Fallback getFallback() { return fallback; }

    public static class Http {
        private Duration connectTimeout = Duration.ofSeconds(5);
        private Duration readTimeout = Duration.ofSeconds(5);

        public Duration getConnectTimeout() { return connectTimeout; }
        public void setConnectTimeout(Duration connectTimeout) { this.connectTimeout = connectTimeout; }

        public Duration getReadTimeout() { return readTimeout; }
        public void setReadTimeout(Duration readTimeout) { this.readTimeout = readTimeout; }
    }

    public static class Fallback {
        private boolean enabled = false;
        private String keyPrefix = "platform:audit:pending:";
        private Duration recoveryInterval = Duration.ofMinutes(5);
        private int batchSize = 50;
        private final Lock lock = new Lock();

        public boolean isEnabled() { return enabled; }
        public void setEnabled(boolean enabled) { this.enabled = enabled; }

        public String getKeyPrefix() { return keyPrefix; }
        public void setKeyPrefix(String keyPrefix) { this.keyPrefix = keyPrefix; }

        public Duration getRecoveryInterval() { return recoveryInterval; }
        public void setRecoveryInterval(Duration recoveryInterval) { this.recoveryInterval = recoveryInterval; }

        public int getBatchSize() { return batchSize; }
        public void setBatchSize(int batchSize) { this.batchSize = batchSize; }

        public Lock getLock() { return lock; }

        public static class Lock {
            private String keyPrefix = "platform:audit:recovery:lock:";
            private Duration ttl = Duration.ofMinutes(2);

            public String getKeyPrefix() { return keyPrefix; }
            public void setKeyPrefix(String keyPrefix) { this.keyPrefix = keyPrefix; }

            public Duration getTtl() { return ttl; }
            public void setTtl(Duration ttl) { this.ttl = ttl; }
        }
    }
}
