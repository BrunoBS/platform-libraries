package br.com.portalmanager.platform.library.scheduling.configuration;

import java.time.Duration;
import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "golden.scheduling.distributed-lock")
public class PlatformSchedulingProperties {
    private boolean enabled = true;
    private Duration lockAtMostFor = Duration.ofMinutes(10);
    private Duration lockAtLeastFor = Duration.ZERO;
    private boolean keepAlive = true;

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public Duration getLockAtMostFor() {
        return lockAtMostFor;
    }

    public void setLockAtMostFor(Duration lockAtMostFor) {
        this.lockAtMostFor = lockAtMostFor;
    }

    public Duration getLockAtLeastFor() {
        return lockAtLeastFor;
    }

    public void setLockAtLeastFor(Duration lockAtLeastFor) {
        this.lockAtLeastFor = lockAtLeastFor;
    }

    public boolean isKeepAlive() {
        return keepAlive;
    }

    public void setKeepAlive(boolean keepAlive) {
        this.keepAlive = keepAlive;
    }
}
