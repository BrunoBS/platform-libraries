package br.com.portalmanager.platform.library.messagequeue.api.contract;

import java.time.Instant;
import java.util.Optional;

/** Trusted, provider-neutral evidence for the message currently being handled on this thread. */
public final class MessageQueueDeliveryContext {
    private static final ThreadLocal<Delivery> CURRENT = new ThreadLocal<>();

    private MessageQueueDeliveryContext() { }

    public static Optional<Delivery> current() { return Optional.ofNullable(CURRENT.get()); }

    /** Opens a scoped delivery context. Infrastructure adapters must always close the returned scope. */
    public static Scope open(Delivery delivery) {
        Delivery previous = CURRENT.get();
        CURRENT.set(delivery);
        return () -> { if (previous == null) CURRENT.remove(); else CURRENT.set(previous); };
    }

    public record Delivery(String provider, String entryPoint, String destination, String resourceIdentifier,
            String transportMessageId, String messageId, String correlationId, Instant timestamp) { }

    @FunctionalInterface public interface Scope extends AutoCloseable { @Override void close(); }
}
