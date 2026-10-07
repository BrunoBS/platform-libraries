package br.com.portalmanager.platform.library.testing.cloud.aws;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Target({})
@Retention(RetentionPolicy.RUNTIME)
public @interface AwsS3 {

    String[] buckets() default {};
}
