package com.empresa.platform.authorization.resource;

import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.repository.QueryRewriter;

/**
 * PoC native-query visibility strategy.
 *
 * <p>The authorization values are bound to the current JDBC/MySQL session by
 * {@link ResourceVisibilityFilterManager}. This rewriter adds no user data or
 * bind markers to the repository query; it only applies a fixed predicate over
 * the projection's {@code authorizerGroup} column.</p>
 *
 * <p>For this PoC the native query is wrapped as a derived table. A protected
 * query that does not expose {@code authorizerGroup} fails closed at the
 * database instead of silently bypassing visibility.</p>
 */
public class ResourceVisibilityNativeQueryRewriter implements QueryRewriter {

    @Override
    public String rewrite(String query, Sort sort) {
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
