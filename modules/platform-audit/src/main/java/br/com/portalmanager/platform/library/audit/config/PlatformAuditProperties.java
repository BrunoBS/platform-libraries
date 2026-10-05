package br.com.portalmanager.platform.library.audit.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "platform.audit")
public class PlatformAuditProperties {

    private boolean enabled = true;
    private String serviceName;
    private int maxEventSizeBytes = 64 * 1024;
    private int maxEventsPerInvocation = 100;

    public boolean isEnabled() { return enabled; }
    public void setEnabled(boolean enabled) { this.enabled = enabled; }
    public String getServiceName() { return serviceName; }
    public void setServiceName(String serviceName) { this.serviceName = serviceName; }
    public int getMaxEventSizeBytes() { return maxEventSizeBytes; }
    public void setMaxEventSizeBytes(int maxEventSizeBytes) { this.maxEventSizeBytes = maxEventSizeBytes; }
    public int getMaxEventsPerInvocation() { return maxEventsPerInvocation; }
    public void setMaxEventsPerInvocation(int maxEventsPerInvocation) { this.maxEventsPerInvocation = maxEventsPerInvocation; }
}
