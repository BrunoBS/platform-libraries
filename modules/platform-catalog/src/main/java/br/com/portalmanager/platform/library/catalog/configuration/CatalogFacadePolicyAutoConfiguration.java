package br.com.portalmanager.platform.library.catalog.configuration;

import br.com.portalmanager.platform.library.catalog.api.facade.AbstractCatalogFacade;
import br.com.portalmanager.platform.library.catalog.internal.facade.CatalogFacadePolicyValidator;
import org.springframework.beans.factory.SmartInitializingSingleton;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;

@AutoConfiguration
public class CatalogFacadePolicyAutoConfiguration {
    @Bean
    SmartInitializingSingleton catalogFacadePolicyValidator(ApplicationContext context) {
        return () -> context.getBeansOfType(AbstractCatalogFacade.class)
                .values().forEach(CatalogFacadePolicyValidator::validate);
    }
}
