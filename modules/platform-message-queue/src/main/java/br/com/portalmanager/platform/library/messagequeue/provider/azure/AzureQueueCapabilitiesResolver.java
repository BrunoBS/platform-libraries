package br.com.portalmanager.platform.library.messagequeue.provider.azure;

import br.com.portalmanager.platform.library.messagequeue.capability.QueueCapabilities;
import br.com.portalmanager.platform.library.messagequeue.capability.QueueCapabilitiesResolver;
import com.azure.messaging.servicebus.administration.ServiceBusAdministrationClient;

public class AzureQueueCapabilitiesResolver implements QueueCapabilitiesResolver {

    private final ServiceBusAdministrationClient administrationClient;

    public AzureQueueCapabilitiesResolver(ServiceBusAdministrationClient administrationClient) {
        this.administrationClient = administrationClient;
    }

    @Override
    public QueueCapabilities resolve(String queue) {
        var properties = administrationClient.getQueue(queue);
        return new QueueCapabilities(properties.isSessionRequired(), false);
    }
}
