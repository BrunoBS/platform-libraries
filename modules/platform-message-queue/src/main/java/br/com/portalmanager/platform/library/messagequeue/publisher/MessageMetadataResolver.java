package br.com.portalmanager.platform.library.messagequeue.publisher;

import br.com.portalmanager.platform.library.messagequeue.annotation.QueueMessage;

record MessageMetadataResolver() {

    MessageMetadata resolve(String destination, Object payload) {
        if (payload == null) {
            throw new IllegalArgumentException("Message queue payload is required");
        }

        QueueMessage annotation = payload.getClass().getAnnotation(QueueMessage.class);
        if (annotation == null) {
            return new MessageMetadata(destination, "1");
        }

        String type = annotation.type().isBlank() ? destination : annotation.type();
        String version = annotation.version().isBlank() ? "1" : annotation.version();
        return new MessageMetadata(type, version);
    }

    record MessageMetadata(String type, String version) {
    }
}
