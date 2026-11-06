package br.com.portalmanager.platform.library.messagequeue.internal.capability;

public interface QueueCapabilitiesResolver {

    QueueCapabilities resolve(String queue);
}
