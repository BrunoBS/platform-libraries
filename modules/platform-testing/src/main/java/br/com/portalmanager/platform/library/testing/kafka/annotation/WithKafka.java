package br.com.portalmanager.platform.library.testing.kafka.annotation;

import br.com.portalmanager.platform.library.testing.container.PlatformTestingContainerImages;
import br.com.portalmanager.platform.library.testing.kafka.KafkaTestConfiguration;

import org.springframework.context.annotation.Import;

import java.lang.annotation.ElementType;
import java.lang.annotation.Inherited;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@Inherited
@Import(KafkaTestConfiguration.class)
public @interface WithKafka {

    String image() default PlatformTestingContainerImages.KAFKA;

    Topic[] topics() default {};

    @Target({})
    @Retention(RetentionPolicy.RUNTIME)
    @interface Topic {
        String name();

        int partitions() default 1;
    }
}
