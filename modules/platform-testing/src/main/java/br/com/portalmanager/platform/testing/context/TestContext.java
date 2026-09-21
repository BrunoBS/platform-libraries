package br.com.portalmanager.platform.testing.context;

import java.util.UUID;

public final class TestContext {

    private static final ThreadLocal<String> CORRELATION_ID = new ThreadLocal<>();

    private TestContext() {
    }

    public static String correlationId() {
        String current = CORRELATION_ID.get();
        if (current == null) {
            current = UUID.randomUUID().toString();
            CORRELATION_ID.set(current);
        }
        return current;
    }

    public static void reset() {
        CORRELATION_ID.remove();
    }
}
