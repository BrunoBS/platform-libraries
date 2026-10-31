package br.com.portalmanager.platform.library.authorization.model;

/** Evidence observed by a trusted transport integration, never bound from a request or message header. */
public record AuthorizationEntryOrigin(String entryPoint, String destination, String queueUrl,
        String messageId, String correlationId, java.time.Instant timestamp) { }
