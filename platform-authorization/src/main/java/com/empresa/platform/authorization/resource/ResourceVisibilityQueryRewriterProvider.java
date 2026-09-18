package com.empresa.platform.authorization.resource;

import org.springframework.data.jpa.repository.QueryRewriter;
import org.springframework.data.jpa.repository.query.JpaQueryMethod;
import org.springframework.data.jpa.repository.query.QueryRewriterProvider;

/**
 * Selects the platform native visibility rewriter for native queries that did
 * not explicitly opt into another rewriter.
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
        if (method.getQueryRewriter() != QueryRewriter.IdentityQueryRewriter.class) {
            return delegate.getQueryRewriter(method);
        }

        if (method.getAnnotatedQuery() != null && method.getRequiredDeclaredQuery().isNative()) {
            return visibilityRewriter;
        }

        return delegate.getQueryRewriter(method);
    }
}
