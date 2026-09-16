package com.empresa.platform.audit.recovery;

import com.empresa.platform.audit.config.PlatformAuditProperties;
import com.empresa.platform.audit.fallback.AuditFallbackStore;
import com.empresa.platform.audit.model.AuditEventRequest;
import com.empresa.platform.audit.publisher.AuditPublisher;
import org.junit.jupiter.api.Test;
import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler;

import java.time.Instant;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;

class AuditRecoveryServiceTest {

    @Test
    void shouldStopCycleWhenFirstPendingEventStillFails() {
        PlatformAuditProperties properties = properties(10);
        InMemoryFallbackStore store = new InMemoryFallbackStore();
        store.save(event("1"));
        store.save(event("2"));

        AuditPublisher publisher = ignored -> {
            throw new IllegalStateException("audit unavailable");
        };

        AuditRecoveryService recovery = recoveryService(publisher, store, new AlwaysAvailableLock(), properties);
        recovery.recover();

        assertThat(store.size()).isEqualTo(2);
    }

    @Test
    void shouldSkipRecoveryWhenDistributedLockIsUnavailable() {
        PlatformAuditProperties properties = properties(10);
        InMemoryFallbackStore store = new InMemoryFallbackStore();
        store.save(event("1"));

        AtomicInteger published = new AtomicInteger();
        AuditPublisher publisher = ignored -> published.incrementAndGet();

        AuditRecoveryService recovery = recoveryService(publisher, store, new UnavailableLock(), properties);
        recovery.recover();

        assertThat(published).hasValue(0);
        assertThat(store.size()).isEqualTo(1);
    }

    @Test
    void shouldRecoverUpToConfiguredBatchSize() {
        PlatformAuditProperties properties = properties(2);
        InMemoryFallbackStore store = new InMemoryFallbackStore();
        store.save(event("1"));
        store.save(event("2"));
        store.save(event("3"));

        AtomicInteger published = new AtomicInteger();
        AuditPublisher publisher = ignored -> published.incrementAndGet();

        AuditRecoveryService recovery = recoveryService(publisher, store, new AlwaysAvailableLock(), properties);
        recovery.recover();

        assertThat(published).hasValue(2);
        assertThat(store.size()).isEqualTo(1);
        assertThat(store.peek().resourceId()).isEqualTo("3");
    }

    private AuditRecoveryService recoveryService(
            AuditPublisher publisher,
            AuditFallbackStore store,
            AuditRecoveryLock lock,
            PlatformAuditProperties properties
    ) {
        return new AuditRecoveryService(
                publisher,
                store,
                lock,
                new ThreadPoolTaskScheduler(),
                properties
        );
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

    private static final class UnavailableLock implements AuditRecoveryLock {
        @Override
        public Optional<String> tryAcquire() {
            return Optional.empty();
        }

        @Override
        public void release(String token) {
        }
    }

    private static final class AlwaysAvailableLock implements AuditRecoveryLock {
        @Override
        public Optional<String> tryAcquire() {
            return Optional.of("test-token");
        }

        @Override
        public void release(String token) {
        }
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
