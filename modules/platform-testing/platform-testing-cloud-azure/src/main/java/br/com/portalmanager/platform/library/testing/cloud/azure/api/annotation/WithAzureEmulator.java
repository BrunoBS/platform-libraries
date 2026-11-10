package br.com.portalmanager.platform.library.testing.cloud.azure.api.annotation;

import br.com.portalmanager.platform.library.testing.cloud.azure.api.blob.annotation.AzureBlobStorage;
import br.com.portalmanager.platform.library.testing.cloud.azure.api.servicebus.annotation.AzureServiceBus;
import br.com.portalmanager.platform.library.testing.cloud.azure.internal.configuration.AzureEmulatorImportRegistrar;

import org.springframework.context.annotation.Import;

import java.lang.annotation.ElementType;
import java.lang.annotation.Inherited;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@Inherited
@Import(AzureEmulatorImportRegistrar.class)
public @interface WithAzureEmulator {

    AzureServiceBus[] serviceBus() default {};

    AzureBlobStorage[] blobStorage() default {};
}
