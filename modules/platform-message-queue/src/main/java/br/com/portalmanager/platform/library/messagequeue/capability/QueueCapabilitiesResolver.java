package br.com.portalmanager.platform.library.messagequeue.capability;

public interface QueueCapabilitiesResolver {

    QueueCapabilities resolve(String queue);
}
