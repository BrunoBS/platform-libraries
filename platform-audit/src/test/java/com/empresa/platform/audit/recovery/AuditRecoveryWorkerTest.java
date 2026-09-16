package com.empresa.platform.audit.recovery;

import com.empresa.platform.audit.client.AuditEventClient;
import com.empresa.platform.audit.config.PlatformAuditProperties;
import com.empresa.platform.audit.fallback.AuditFallbackStore;
import com.empresa.platform.audit.model.AuditEventRequest;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;

class AuditRecoveryWorkerTest {

    @Test
    void shouldStopCycleWhenFirstPendingEventStillFails() {
        PlatformAuditProperties properties = properties(10);
        InMemoryFallbackStore store = new InMemoryFallbackStore();
        store.save(event("1"));
        store.save(event("2"));

        AuditEventClient client = ignored -> {
            throw new IllegalStateException("audit unavailable");
        };

        AuditRecoveryWorker worker = new AuditRecoveryWorker(client, store, properties);
        worker.recover();

        assertThat(store.size()).isEqualTo(2);
    }

    @Test
    void shouldRecoverUpToConfiguredBatchSize() {
        PlatformAuditProperties properties = properties(2);
        InMemoryFallbackStore store = new InMemoryFallbackStore();
        store.save(event("1"));
        store.save(event("2"));
        store.save(event("3"));

        AtomicInteger published = new AtomicInteger();
        AuditEventClient client = ignored -> published.incrementAndGet();

        AuditRecoveryWorker worker = new AuditRecoveryWorker(client, store, properties);
        worker.recover();

        assertThat(published).hasValue(2);
        assertThat(store.size()).isEqualTo(1);
        assertThat(store.peek().resourceId()).isEqualTo("3");
    }

    private PlatformAuditProperties properties(int batchSize) {
        PlatformAuditProperties properties = new PlatformAuditProperties();
        properties.getFallback().setBatchSize(batchSize);
        return properties;
    }

    private AuditEventRequest event(String id) {
        return new AuditEventRequest(
                Instant.now(),
                "account",
                "account-1",
                "application-1",
                "dev",
                "account",
                id,
                "UPDATE",
                "user",
                "correlation-" + id,
                200,
                Map.of("id", id),
                Map.of()
        );
    }

    private static final class InMemoryFallbackStore implements AuditFallbackStore {

        private final Deque<AuditEventRequest> events = new ArrayDeque<>();

        @Override
        public void save(AuditEventRequest event) {
            events.addLast(event);
        }

        @Override
        public AuditEventRequest peek() {
            return events.peekFirst();
        }

        @Override
        public void removeHead() {
            events.pollFirst();
        }

        @Override
        public boolean hasPending() {
            return !events.isEmpty();
        }

        int size() {
            return events.size();
        }
    }
}
