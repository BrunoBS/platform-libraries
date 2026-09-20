package com.empresa.platform.testing.annotation;

import com.empresa.platform.testing.kafka.KafkaTestConfiguration;
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
}
