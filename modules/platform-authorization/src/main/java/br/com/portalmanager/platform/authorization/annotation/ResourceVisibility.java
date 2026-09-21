package br.com.portalmanager.platform.authorization.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Applies visibility rules to resources returned by the annotated method.
 *
 * <p>This annotation does not authorize execution of the endpoint. Endpoint
 * access must be controlled separately with {@link AuthorizationRequired}.</p>
 *
 * <p>For collections, resources whose authorizer group is not present in the
 * current user session are filtered out. For a single resource, access is
 * denied when the resource is not visible to the current user. OWNER users
 * bypass visibility filtering.</p>
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface ResourceVisibility {
}
