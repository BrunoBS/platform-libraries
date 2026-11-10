package br.com.portalmanager.platform.library.audit.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "platform.audit")
public class PlatformAuditProperties {

    private boolean enabled = true;
    private String serviceName;
    private final Outbox outbox = new Outbox();

    public boolean isEnabled() { return enabled; }
    public void setEnabled(boolean enabled) { this.enabled = enabled; }
    public String getServiceName() { return serviceName; }
    public void setServiceName(String serviceName) { this.serviceName = serviceName; }
    public Outbox getOutbox() { return outbox; }

    public static class Outbox {
        private String destination;
        private String keyPrefix = "platform-audit/";
        private int batchSize = 50;
        private int maximumAttempts = 10;
        private int maximumRecoveryDays = 7;
        private int initialAttempts = 3;
        private java.time.Duration shortRetryDelay = java.time.Duration.ofMinutes(10);
        private java.time.Duration dailyRetryDelay = java.time.Duration.ofDays(1);
        private java.time.Duration processingLease = java.time.Duration.ofMinutes(30);

        public String getDestination() { return destination; }
        public void setDestination(String destination) { this.destination = destination; }
        public String getKeyPrefix() { return keyPrefix; }
        public void setKeyPrefix(String keyPrefix) { this.keyPrefix = keyPrefix; }
        public int getBatchSize() { return batchSize; }
        public void setBatchSize(int batchSize) { this.batchSize = batchSize; }
        public int getMaximumAttempts() { return maximumAttempts; }
        public void setMaximumAttempts(int maximumAttempts) { this.maximumAttempts = maximumAttempts; }
        public int getMaximumRecoveryDays() { return maximumRecoveryDays; }
        public void setMaximumRecoveryDays(int maximumRecoveryDays) { this.maximumRecoveryDays = maximumRecoveryDays; }
        public int getInitialAttempts() { return initialAttempts; }
        public void setInitialAttempts(int initialAttempts) { this.initialAttempts = initialAttempts; }
        public java.time.Duration getShortRetryDelay() { return shortRetryDelay; }
        public void setShortRetryDelay(java.time.Duration shortRetryDelay) { this.shortRetryDelay = shortRetryDelay; }
        public java.time.Duration getDailyRetryDelay() { return dailyRetryDelay; }
        public void setDailyRetryDelay(java.time.Duration dailyRetryDelay) { this.dailyRetryDelay = dailyRetryDelay; }
        public java.time.Duration getProcessingLease() { return processingLease; }
        public void setProcessingLease(java.time.Duration processingLease) { this.processingLease = processingLease; }
    }
}
