package br.com.portalmanager.platform.library.objectstorage.api.configuration;

import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.context.properties.EnableConfigurationProperties;

@AutoConfiguration
@EnableConfigurationProperties(ObjectStorageProperties.class)
public class ObjectStorageAutoConfiguration {
}
