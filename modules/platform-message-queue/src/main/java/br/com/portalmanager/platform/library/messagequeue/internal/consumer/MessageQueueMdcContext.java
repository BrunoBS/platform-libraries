package br.com.portalmanager.platform.library.messagequeue.internal.consumer;

import br.com.portalmanager.platform.library.messagequeue.api.contract.MessageQueueMessage;
import org.slf4j.MDC;

import java.util.List;
import java.util.Map;

/** Installs the same message identifiers in the logging context for every provider. */
public final class MessageQueueMdcContext {

    private static final List<String> REQUEST_CONTEXT_KEYS = List.of(
            "correlationId", "messageId", "username", "accountId", "applicationId",
            "environmentId", "workspaceIdentifier", "clientIp", "userAgent", "uri");

    private MessageQueueMdcContext() {
    }

    public static void run(MessageQueueMessage<?> message, Runnable action) {
        Map<String, String> previous = MDC.getCopyOfContextMap();
        try {
            REQUEST_CONTEXT_KEYS.forEach(MDC::remove);
            putIdentifier("correlationId", message.correlationId());
            putIdentifier("messageId", message.messageId());
            action.run();
        } finally {
            if (previous == null) {
                MDC.clear();
            } else {
                MDC.setContextMap(previous);
            }
        }
    }

    private static void putIdentifier(String key, String value) {
        if (value != null && !value.isBlank() && value.length() <= 128
                && value.chars().noneMatch(Character::isISOControl)) {
            MDC.put(key, value);
        }
    }
}
