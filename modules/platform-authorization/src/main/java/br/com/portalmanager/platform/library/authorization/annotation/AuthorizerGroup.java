package br.com.portalmanager.platform.library.authorization.annotation;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Marks the persistent attribute that stores the resource authorizer group.
 *
 * <p>The platform resolves the physical database column through Hibernate
 * metadata, so neither the Java attribute name nor the column name needs to
 * follow a fixed naming convention.</p>
 */
@Documented
@Target(ElementType.FIELD)
@Retention(RetentionPolicy.RUNTIME)
public @interface AuthorizerGroup {
}
