package br.com.portalmanager.platform.library.authorization.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Applies resource visibility to the annotated use-case method.
 *
 * <p>The source metadata is used only by the platform library to resolve the
 * internal resource ids visible to the current user's authorizer groups.
 * The business repository remains unaware of authorization filtering.</p>
 *
 * <p>Table and column names are validated as SQL identifiers before use.
 * Values coming from the user session are always bound as query parameters.</p>
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface ResourceVisibility {

    /**
     * Table (or schema-qualified table) that maps a resource id to an authorizer group.
     * Example: {@code workspace_authorization}.
     */
    String table();

    /**
     * Column in {@link #table()} containing the resource internal BIGINT id.
     * Example: {@code workspace_id}.
     */
    String resourceIdColumn();

    /**
     * Column in {@link #table()} containing the authorizer group.
     * Example: {@code authorizer_group}.
     */
    String authorizerGroupColumn();
}
