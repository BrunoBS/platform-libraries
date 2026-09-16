package com.empresa.platform.audit.publisher;

import com.empresa.platform.audit.client.AuditEventClient;
import com.empresa.platform.audit.config.PlatformAuditProperties;
import com.empresa.platform.audit.model.AuditEventRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.task.TaskExecutor;

public final class DefaultAuditPublisher implements AuditPublisher {

    private static final Logger log = LoggerFactory.getLogger(DefaultAuditPublisher.class);

    private final AuditEventClient client;
    private final TaskExecutor taskExecutor;
    private final PlatformAuditProperties properties;

    public DefaultAuditPublisher(
            AuditEventClient client,
            TaskExecutor taskExecutor,
            PlatformAuditProperties properties
    ) {
        this.client = client;
        this.taskExecutor = taskExecutor;
        this.properties = properties;
    }

    @Override
    public void publish(AuditEventRequest event) {
        if (properties.isFailOnError()) {
            client.publish(event);
            return;
        }

        taskExecutor.execute(() -> {
            try {
                client.publish(event);
            } catch (Exception exception) {
                log.error(
                        "Failed to publish audit event | resource={} | resourceId={} | action={}",
                        event.resource(),
                        event.resourceId(),
                        event.action(),
                        exception
                );
            }
        });
    }
}
