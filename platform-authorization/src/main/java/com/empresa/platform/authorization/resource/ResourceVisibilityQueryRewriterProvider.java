package com.empresa.platform.authorization.resource;

import org.springframework.data.jpa.repository.QueryRewriter;
import org.springframework.data.jpa.repository.query.JpaQueryMethod;
import org.springframework.data.jpa.repository.query.QueryRewriterProvider;

/**
 * Applies platform visibility to every declared native query. When an application
 * declares a custom QueryRewriter, its transformation runs first and visibility
 * is applied to the resulting SQL.
 */
public class ResourceVisibilityQueryRewriterProvider implements QueryRewriterProvider {

    private final QueryRewriterProvider delegate;
    private final ResourceVisibilityNativeQueryRewriter visibilityRewriter;

    public ResourceVisibilityQueryRewriterProvider(QueryRewriterProvider delegate,
                                                   ResourceVisibilityNativeQueryRewriter visibilityRewriter) {
        this.delegate = delegate;
        this.visibilityRewriter = visibilityRewriter;
    }

    @Override
    public QueryRewriter getQueryRewriter(JpaQueryMethod method) {
        QueryRewriter applicationRewriter = delegate.getQueryRewriter(method);

        if (method.getAnnotatedQuery() == null
                || method.getQueryAnnotation() == null
                || !method.getQueryAnnotation().nativeQuery()) {
            return applicationRewriter;
        }

        if (method.getQueryRewriter() == QueryRewriter.IdentityQueryRewriter.class) {
            return visibilityRewriter;
        }

        return (query, sort) ->
                visibilityRewriter.rewrite(applicationRewriter.rewrite(query, sort), sort);
    }
}
