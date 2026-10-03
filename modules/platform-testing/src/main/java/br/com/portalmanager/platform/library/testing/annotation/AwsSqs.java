package br.com.portalmanager.platform.library.testing.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Target({})
@Retention(RetentionPolicy.RUNTIME)
public @interface AwsSqs {

    String[] queues() default {};

    RedrivePolicy[] redrivePolicies() default {};

    @Target({})
    @Retention(RetentionPolicy.RUNTIME)
    @interface RedrivePolicy {
        String sourceQueue();

        String deadLetterQueue();

        int maxReceiveCount() default 5;
    }
}
