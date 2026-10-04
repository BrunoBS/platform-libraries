package br.com.portalmanager.platform.library.testing.cloud.azure;

import com.azure.messaging.servicebus.administration.ServiceBusAdministrationClient;
import com.azure.messaging.servicebus.administration.ServiceBusAdministrationClientBuilder;
import org.springframework.beans.factory.FactoryBean;

public final class AzureServiceBusAdministrationClientFactoryBean
        implements FactoryBean<ServiceBusAdministrationClient> {

    private final AzureServiceBusContainer container;
    private ServiceBusAdministrationClient client;

    public AzureServiceBusAdministrationClientFactoryBean(AzureServiceBusContainer container) {
        this.container = container;
    }

    @Override
    public ServiceBusAdministrationClient getObject() {
        if (client == null) {
            client = new ServiceBusAdministrationClientBuilder()
                    .connectionString(container.emulator().getConnectionString())
                    .buildClient();
        }
        return client;
    }

    @Override
    public Class<?> getObjectType() {
        return ServiceBusAdministrationClient.class;
    }

    @Override
    public boolean isSingleton() {
        return true;
    }
}
