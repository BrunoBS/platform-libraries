package br.com.portalmanager.platform.library.testing.cloud.azure.blob;

import br.com.portalmanager.platform.library.testing.cloud.azure.blobservicebus.AzureBlobServiceClientFactoryBean;
import com.azure.storage.blob.BlobServiceClient;
import org.springframework.beans.factory.FactoryBean;
import org.springframework.beans.factory.support.AbstractBeanDefinition;
import org.springframework.beans.factory.support.BeanDefinitionRegistry;
import org.springframework.beans.factory.support.RootBeanDefinition;

public final class AzureBlobStorageTestSupport {

    private static final String BLOB_CLIENT_BEAN = "azureBlobServiceClient";

    public AzureBlobStorageTestSupport() {
    }

    public void register(BeanDefinitionRegistry registry) {
        RootBeanDefinition definition = new RootBeanDefinition(AzureBlobServiceClientFactoryBean.class);
        definition.setAutowireMode(AbstractBeanDefinition.AUTOWIRE_CONSTRUCTOR);
        definition.setAttribute(FactoryBean.OBJECT_TYPE_ATTRIBUTE, BlobServiceClient.class);
        registry.registerBeanDefinition(BLOB_CLIENT_BEAN, definition);
    }
}
