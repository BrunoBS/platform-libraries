package br.com.portalmanager.platform.library.testing.cloud.azure.blob.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Target({})
@Retention(RetentionPolicy.RUNTIME)
public @interface AzureBlobStorage {

    String[] containers() default {};

    /**
     * When configured, successful Blob uploads publish an Event Grid compatible
     * BlobCreated message to this Service Bus queue in tests.
     */
    String blobCreatedQueue() default "";
}
