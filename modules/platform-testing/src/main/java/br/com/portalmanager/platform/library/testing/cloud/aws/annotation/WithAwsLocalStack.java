package br.com.portalmanager.platform.library.testing.cloud.aws.annotation;

import br.com.portalmanager.platform.library.testing.cloud.aws.AwsLocalStackImportRegistrar;

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

    AwsSqs[] sqs() default {};

    AwsS3[] s3() default {};
}
