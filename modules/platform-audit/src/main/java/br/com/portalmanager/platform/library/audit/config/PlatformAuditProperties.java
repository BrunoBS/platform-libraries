package br.com.portalmanager.platform.library.audit.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.Set;

@ConfigurationProperties(prefix = "platform.audit")
public class PlatformAuditProperties {

    private boolean enabled = true;
    private String serviceName;
    private String destination = "audit-events";
    private boolean failOnError = true;
    private Set<String> allowedHeaders = Set.of("correlation-id");
    private int maxEventSizeBytes = 64 * 1024;
    private int maxEventsPerInvocation = 100;

    public boolean isEnabled() { return enabled; }
    public void setEnabled(boolean enabled) { this.enabled = enabled; }
    public String getServiceName() { return serviceName; }
    public void setServiceName(String serviceName) { this.serviceName = serviceName; }
    public String getDestination() { return destination; }
    public void setDestination(String destination) { this.destination = destination; }
    public boolean isFailOnError() { return failOnError; }
    public void setFailOnError(boolean failOnError) { this.failOnError = failOnError; }
    public Set<String> getAllowedHeaders() { return allowedHeaders; }
    public void setAllowedHeaders(Set<String> allowedHeaders) { this.allowedHeaders = allowedHeaders; }
    public int getMaxEventSizeBytes() { return maxEventSizeBytes; }
    public void setMaxEventSizeBytes(int maxEventSizeBytes) { this.maxEventSizeBytes = maxEventSizeBytes; }
    public int getMaxEventsPerInvocation() { return maxEventsPerInvocation; }
    public void setMaxEventsPerInvocation(int maxEventsPerInvocation) { this.maxEventsPerInvocation = maxEventsPerInvocation; }
}
