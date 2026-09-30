package br.com.portalmanager.platform.library.authorization.annotation;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Enables platform-managed resource visibility for the annotated use-case method.
 *
 * <p>The platform library resolves the current user's authorizer groups and
 * enables the Hibernate filter before the business query executes.</p>
 *
 * <p>Entities participate in visibility by declaring exactly one persistent
 * field annotated with {@link AuthorizerGroup}. The physical database column is
 * resolved from Hibernate metadata, so neither the Java attribute name nor the
 * column name is fixed by the library.</p>
 */
@Documented
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface ResourceVisibility {
}
