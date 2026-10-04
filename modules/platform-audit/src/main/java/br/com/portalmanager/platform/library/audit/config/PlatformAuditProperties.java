package br.com.portalmanager.platform.library.audit.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "platform.audit")
public class PlatformAuditProperties {

    private boolean enabled = true;
    private String serviceName = "unknown";
    private String destination = "audit-events";
    private boolean failOnError = false;

    public boolean isEnabled() { return enabled; }
    public void setEnabled(boolean enabled) { this.enabled = enabled; }
    public String getServiceName() { return serviceName; }
    public void setServiceName(String serviceName) { this.serviceName = serviceName; }
    public String getDestination() { return destination; }
    public void setDestination(String destination) { this.destination = destination; }
    public boolean isFailOnError() { return failOnError; }
    public void setFailOnError(boolean failOnError) { this.failOnError = failOnError; }
}
