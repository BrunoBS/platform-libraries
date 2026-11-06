package br.com.portalmanager.platform.library.messagequeue.internal.message;

import br.com.portalmanager.platform.library.messagequeue.api.annotation.QueueMessage;

/** Resolves the same effective type and version for both publishing and consuming. */
public final class MessageMetadataResolver {

    private MessageMetadataResolver() {
    }

    public static Metadata resolve(String destination, Class<?> payloadType) {
        QueueMessage annotation = payloadType.getAnnotation(QueueMessage.class);
        String type = annotation == null || annotation.type().isBlank()
                ? destination
                : annotation.type();
        String version = annotation == null || annotation.version().isBlank()
                ? "1"
                : annotation.version();
        return new Metadata(type, version);
    }

    public record Metadata(String type, String version) {
    }
}
