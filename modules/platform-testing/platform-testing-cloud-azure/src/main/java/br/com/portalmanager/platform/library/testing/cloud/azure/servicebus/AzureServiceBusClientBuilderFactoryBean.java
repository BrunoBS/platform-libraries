package br.com.portalmanager.platform.library.testing.cloud.azure.servicebus;

import com.azure.messaging.servicebus.ServiceBusClientBuilder;
import org.springframework.beans.factory.FactoryBean;

public final class AzureServiceBusClientBuilderFactoryBean implements FactoryBean<ServiceBusClientBuilder> {

    private final AzureServiceBusContainer container;
    private ServiceBusClientBuilder builder;

    public AzureServiceBusClientBuilderFactoryBean(AzureServiceBusContainer container) {
        this.container = container;
    }

    @Override
    public ServiceBusClientBuilder getObject() {
        if (builder == null) {
            builder = new ServiceBusClientBuilder()
                    .connectionString(container.emulator().getConnectionString());
        }
        return builder;
    }

    @Override
    public Class<?> getObjectType() {
        return ServiceBusClientBuilder.class;
    }

    @Override
    public boolean isSingleton() {
        return true;
    }
}
