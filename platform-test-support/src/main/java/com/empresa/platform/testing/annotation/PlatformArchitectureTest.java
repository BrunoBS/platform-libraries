package com.empresa.platform.testing.annotation;

import com.empresa.platform.testing.architecture.PlatformArchitectureExtension;
import org.junit.jupiter.api.extension.ExtendWith;

import java.lang.annotation.ElementType;
import java.lang.annotation.Inherited;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Opt-in architecture guard for consumer services.
 *
 * <p>It detects concrete application classes that override behavior declared by
 * platform base types and requires focused test coverage for those classes.</p>
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@Inherited
@ExtendWith(PlatformArchitectureExtension.class)
public @interface PlatformArchitectureTest {

    String[] basePackages() default {};

    String[] observedBasePackages() default {"com.empresa.platform"};
}
