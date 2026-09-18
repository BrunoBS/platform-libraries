package com.empresa.platform.authorization.resource;

import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.repository.QueryRewriter;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Applies database-side visibility to native Spring Data queries while a
 * {@code @ResourceVisibility} scope is active.
 *
 * The annotated repository parameter is the anchor. This PoC intentionally
 * supports only a simple IN (:parameter) predicate and fails closed for
 * ambiguous or unsupported shapes.
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

    public String rewrite(String query, Sort sort, String visibilityParameter) {
        if (!context.isActive()) {
            return query;
        }
        if (query == null || query.isBlank()) {
            throw new ResourceVisibilityNativeQueryException("Protected native query must not be empty");
        }
        if (visibilityParameter == null || visibilityParameter.isBlank()) {
            throw new ResourceVisibilityNativeQueryException("Native visibility parameter must not be blank");
        }

        Pattern parameter = Pattern.compile(
                "(?i)([^\\s()]+(?:\\([^)]*\\))?\\s+IN\\s*\\(\\s*:" +
                        Pattern.quote(visibilityParameter) + "\\s*\\))"
        );
        Matcher matcher = parameter.matcher(query);

        if (!matcher.find()) {
            throw new ResourceVisibilityNativeQueryException(
                    "Native visibility query must contain an IN (:" + visibilityParameter + ") predicate"
            );
        }
        if (matcher.find()) {
            throw new ResourceVisibilityNativeQueryException(
                    "Native visibility parameter :" + visibilityParameter +
                            " must occur in exactly one supported IN predicate"
            );
        }

        // For now normal users keep the explicit predicate. OWNER bypass will be
        // driven by the visibility execution context in the next PoC step.
        return query;
    }
}
