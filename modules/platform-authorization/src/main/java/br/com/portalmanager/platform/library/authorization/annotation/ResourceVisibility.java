package br.com.portalmanager.platform.library.authorization.annotation;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Enables platform-managed resource visibility for the annotated use-case method.
 *
 * <p>The entity type identifies the resource whose Hibernate visibility filter
 * must be enabled for the invocation. This keeps visibility scoped to that
 * resource and avoids filtering incidental queries of other protected entities
 * in the same persistence context.</p>
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

    Class<?> value();
}
