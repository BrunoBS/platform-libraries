package br.com.portalmanager.platform.library.testing.cloud.azure.servicebus;

import br.com.portalmanager.platform.library.testing.cloud.azure.AzureServiceTestSupport;
import com.azure.messaging.servicebus.ServiceBusClientBuilder;
import org.springframework.beans.factory.FactoryBean;
import org.springframework.beans.factory.support.AbstractBeanDefinition;
import org.springframework.beans.factory.support.BeanDefinitionRegistry;
import org.springframework.beans.factory.support.RootBeanDefinition;

public final class AzureServiceBusTestSupport implements AzureServiceTestSupport {

    private static final String SERVICE_BUS_CLIENT_BUILDER_BEAN = "azureServiceBusClientBuilder";

    public AzureServiceBusTestSupport() {
    }

    @Override
    public void register(BeanDefinitionRegistry registry) {
        RootBeanDefinition definition = new RootBeanDefinition(AzureServiceBusClientBuilderFactoryBean.class);
        definition.setAutowireMode(AbstractBeanDefinition.AUTOWIRE_CONSTRUCTOR);
        definition.setAttribute(FactoryBean.OBJECT_TYPE_ATTRIBUTE, ServiceBusClientBuilder.class);
        registry.registerBeanDefinition(SERVICE_BUS_CLIENT_BUILDER_BEAN, definition);
    }
}
