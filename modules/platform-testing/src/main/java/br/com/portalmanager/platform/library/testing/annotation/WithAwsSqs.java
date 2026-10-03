package br.com.portalmanager.platform.library.testing.annotation;

import br.com.portalmanager.platform.library.testing.messagequeue.aws.AwsSqsTestConfiguration;
import org.springframework.context.annotation.Import;

import java.lang.annotation.ElementType;
import java.lang.annotation.Inherited;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@Inherited
@Import(AwsSqsTestConfiguration.class)
public @interface WithAwsSqs {
}
