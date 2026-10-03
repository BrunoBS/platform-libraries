package br.com.portalmanager.platform.library.testing.cloud.azure;

import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;

@TestConfiguration(proxyBeanMethods = false)
public class AzureBlobStorageTestConfiguration {

    @Bean(initMethod = "start", destroyMethod = "stop")
    AzureBlobStorageContainer azureBlobStorageContainer() {
        return new AzureBlobStorageContainer();
    }
}
