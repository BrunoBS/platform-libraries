package br.com.portalmanager.platform.library.testing.cloud.azure;

import com.azure.messaging.servicebus.ServiceBusClientBuilder;
import com.azure.messaging.servicebus.administration.ServiceBusAdministrationClient;
import org.springframework.beans.factory.FactoryBean;
import org.springframework.beans.factory.support.AbstractBeanDefinition;
import org.springframework.beans.factory.support.BeanDefinitionRegistry;
import org.springframework.beans.factory.support.RootBeanDefinition;

final class AzureServiceBusTestSupport implements AzureServiceTestSupport {

    private static final String SERVICE_BUS_CLIENT_BUILDER_BEAN = "azureServiceBusClientBuilder";

    @Override
    public void register(BeanDefinitionRegistry registry) {
        RootBeanDefinition definition = new RootBeanDefinition(AzureServiceBusClientBuilderFactoryBean.class);
        definition.setAutowireMode(AbstractBeanDefinition.AUTOWIRE_CONSTRUCTOR);
        definition.setAttribute(FactoryBean.OBJECT_TYPE_ATTRIBUTE, ServiceBusClientBuilder.class);
        registry.registerBeanDefinition(SERVICE_BUS_CLIENT_BUILDER_BEAN, definition);

        RootBeanDefinition administrationDefinition =
                new RootBeanDefinition(AzureServiceBusAdministrationClientFactoryBean.class);
        administrationDefinition.setAutowireMode(AbstractBeanDefinition.AUTOWIRE_CONSTRUCTOR);
        administrationDefinition.setAttribute(
                FactoryBean.OBJECT_TYPE_ATTRIBUTE, ServiceBusAdministrationClient.class);
        registry.registerBeanDefinition("azureServiceBusAdministrationClient", administrationDefinition);
    }
}
