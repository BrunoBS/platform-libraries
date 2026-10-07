package br.com.portalmanager.platform.library.testing.cloud.azure.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Target({})
@Retention(RetentionPolicy.RUNTIME)
public @interface AzureServiceBus {

    Queue[] queues() default {};

    @Target({})
    @Retention(RetentionPolicy.RUNTIME)
    @interface Queue {
        String name();

        int maxDeliveryCount() default 3;

        boolean sessionsEnabled() default false;
    }
}
