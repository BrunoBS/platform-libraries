package br.com.portalmanager.platform.library.testing.architecture.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Declares production classes whose customized behavior is covered by a test.
 *
 * <p>Use this when one integration test intentionally covers multiple services,
 * validators or other classes.</p>
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
public @interface CoversClasses {

    Class<?>[] value();
}
