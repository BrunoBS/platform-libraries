package br.com.portalmanager.platform.library.authorization.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Enables platform-managed resource visibility for the annotated use-case method.
 *
 * <p>The resource entity remains free of Hibernate filter annotations. The
 * platform library resolves the current user's authorizer groups and enables
 * the Hibernate filter before the business query executes.</p>
 *
 * <p>POC convention: participating resource tables expose an
 * {@code authorizer_group} column normalized to uppercase.</p>
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface ResourceVisibility {
}
