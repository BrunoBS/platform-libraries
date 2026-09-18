package com.empresa.platform.authorization.resource;

import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.repository.QueryRewriter;

/**
 * Applies database-side visibility to native Spring Data queries while a
 * {@code @ResourceVisibility} scope is active.
 */
public class ResourceVisibilityNativeQueryRewriter implements QueryRewriter {

    private final NativeResourceVisibilityContext context;
    private final NativeResourceVisibilityStrategy strategy;

    public ResourceVisibilityNativeQueryRewriter(
            NativeResourceVisibilityContext context,
            NativeResourceVisibilityStrategy strategy) {
        this.context = context;
        this.strategy = strategy;
    }

    @Override
    public String rewrite(String query, Sort sort) {
        if (!context.isActive()) {
            return query;
        }

        return strategy.apply(query);
    }
}
