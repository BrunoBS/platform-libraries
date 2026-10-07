package br.com.portalmanager.platform.library.testing.cloud.aws.sqs.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Target({})
@Retention(RetentionPolicy.RUNTIME)
public @interface AwsSqs {

    Queue[] queues() default {};

    @Target({})
    @Retention(RetentionPolicy.RUNTIME)
    @interface Queue {
        String name();

        boolean deadLetterEnabled() default false;

        /**
         * Empty uses the standard name {@code <queue-name>-dlq}.
         */
        String deadLetterQueue() default "";

        int maxReceiveCount() default 5;
    }
}
