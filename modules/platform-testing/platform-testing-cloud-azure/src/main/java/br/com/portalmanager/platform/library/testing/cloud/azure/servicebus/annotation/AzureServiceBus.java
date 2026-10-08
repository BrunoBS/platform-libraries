package br.com.portalmanager.platform.library.testing.cloud.azure.servicebus.annotation;

import br.com.portalmanager.platform.library.testing.cloud.azure.AzureContainerImages;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Target({})
@Retention(RetentionPolicy.RUNTIME)
public @interface AzureServiceBus {

    String image() default AzureContainerImages.AZURE_SERVICE_BUS;

    String sqlServerImage() default AzureContainerImages.AZURE_SQL_SERVER;

    Queue[] queues() default {};

    @Target({})
    @Retention(RetentionPolicy.RUNTIME)
    @interface Queue {
        String name();

        int maxDeliveryCount() default 3;

        boolean sessionsEnabled() default false;
    }
}
