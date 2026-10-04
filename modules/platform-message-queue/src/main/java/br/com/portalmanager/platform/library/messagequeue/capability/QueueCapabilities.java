package br.com.portalmanager.platform.library.messagequeue.capability;

public record QueueCapabilities(
        boolean ordered,
        boolean deduplication) {
}
