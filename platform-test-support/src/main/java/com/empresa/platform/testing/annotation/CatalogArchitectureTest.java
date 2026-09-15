package com.empresa.platform.testing.annotation;

import com.empresa.platform.testing.architecture.catalog.CatalogArchitectureExtension;
import org.junit.jupiter.api.extension.ExtendWith;

import java.lang.annotation.ElementType;
import java.lang.annotation.Inherited;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Opt-in architecture guard for services that customize platform catalog behavior.
 *
 * <p>If no base package is informed, the package of the annotated test class is used.</p>
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@Inherited
@ExtendWith(CatalogArchitectureExtension.class)
public @interface CatalogArchitectureTest {

    String[] basePackages() default {};
}
