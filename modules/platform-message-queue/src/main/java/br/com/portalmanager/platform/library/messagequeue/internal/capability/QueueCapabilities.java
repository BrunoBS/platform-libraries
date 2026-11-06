package br.com.portalmanager.platform.library.messagequeue.internal.capability;

public record QueueCapabilities(
        boolean ordered,
        boolean deduplication) {
}
