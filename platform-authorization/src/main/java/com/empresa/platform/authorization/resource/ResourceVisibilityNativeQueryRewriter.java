package com.empresa.platform.authorization.resource;

import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.repository.QueryRewriter;

/**
 * Applies database-side visibility to native Spring Data queries while a
 * {@code @ResourceVisibility} scope is active.
 */
public class ResourceVisibilityNativeQueryRewriter implements QueryRewriter {

    private final NativeResourceVisibilityContext context;

    public ResourceVisibilityNativeQueryRewriter(NativeResourceVisibilityContext context) {
        this.context = context;
    }

    @Override
    public String rewrite(String query, Sort sort) {
        if (!context.isActive()) {
            return query;
        }

        String normalized = query == null ? "" : query.trim();
        if (normalized.isEmpty()) {
            throw new IllegalArgumentException("Protected native query must not be empty");
        }

        return """
                SELECT platform_visibility.*
                FROM (
                %s
                ) platform_visibility
                WHERE JSON_CONTAINS(
                    %s,
                    JSON_QUOTE(LOWER(platform_visibility.authorizerGroup))
                )
                """.formatted(normalized, ResourceVisibilityFilterManager.NATIVE_SESSION_VARIABLE);
    }
}
