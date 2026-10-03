package br.com.portalmanager.platform.library.testing.annotation;

import br.com.portalmanager.platform.library.testing.cloud.aws.AwsLocalStackImportRegistrar;
import br.com.portalmanager.platform.library.testing.cloud.aws.AwsService;
import org.springframework.context.annotation.Import;

import java.lang.annotation.ElementType;
import java.lang.annotation.Inherited;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@Inherited
@Import(AwsLocalStackImportRegistrar.class)
public @interface WithAwsLocalStack {

    AwsService[] services();

    String[] queues() default {};

    String[] buckets() default {};
}
