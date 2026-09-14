package com.empresa.platform.catalog.autoconfigure;

import com.empresa.platform.catalog.message.CatalogMessageProvider;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;

@AutoConfiguration
public class PlatformCatalogAutoConfiguration {

    @Bean
    @ConditionalOnMissingBean
    public CatalogMessageProvider catalogMessageProvider() {
        return new CatalogMessageProvider();
    }
}
