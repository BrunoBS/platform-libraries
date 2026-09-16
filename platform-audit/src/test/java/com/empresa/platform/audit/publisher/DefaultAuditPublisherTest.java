package com.empresa.platform.audit.publisher;

import com.empresa.platform.audit.client.AuditEventClient;
import com.empresa.platform.audit.config.PlatformAuditProperties;
import com.empresa.platform.audit.model.AuditEventRequest;
import org.junit.jupiter.api.Test;
import org.springframework.core.task.SyncTaskExecutor;

import java.time.Instant;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class DefaultAuditPublisherTest {

    private final AuditEventRequest event = new AuditEventRequest(
            Instant.now(),
            "account",
            "account-1",
            "application-1",
            "dev",
            "account",
            "123",
            "UPDATE",
            "user",
            "correlation-1",
            200,
            Map.of("id", "123"),
            Map.of()
    );

    @Test
    void shouldNotPropagateClientFailureByDefault() {
        PlatformAuditProperties properties = new PlatformAuditProperties();
        AuditEventClient client = ignored -> {
            throw new IllegalStateException("audit unavailable");
        };

        DefaultAuditPublisher publisher = new DefaultAuditPublisher(
                client,
                new SyncTaskExecutor(),
                properties
        );

        assertThatCode(() -> publisher.publish(event)).doesNotThrowAnyException();
    }

    @Test
    void shouldPropagateClientFailureWhenFailOnErrorIsEnabled() {
        PlatformAuditProperties properties = new PlatformAuditProperties();
        properties.setFailOnError(true);

        AuditEventClient client = ignored -> {
            throw new IllegalStateException("audit unavailable");
        };

        DefaultAuditPublisher publisher = new DefaultAuditPublisher(
                client,
                new SyncTaskExecutor(),
                properties
        );

        assertThatThrownBy(() -> publisher.publish(event))
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("audit unavailable");
    }
}
