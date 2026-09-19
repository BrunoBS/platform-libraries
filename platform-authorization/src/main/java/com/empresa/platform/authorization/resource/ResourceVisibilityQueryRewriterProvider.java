package com.empresa.platform.authorization.resource;

import com.empresa.platform.authorization.annotation.ResourceVisibilityGroups;
import org.springframework.data.jpa.repository.QueryRewriter;
import org.springframework.data.jpa.repository.query.JpaQueryMethod;
import org.springframework.data.jpa.repository.query.QueryRewriterProvider;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

/**
 * Applies platform visibility to declared native queries.
 *
 * Native visibility is convention based: the repository method declares exactly one
 * parameter annotated with {@link ResourceVisibilityGroups} and {@link Param}. The
 * parameter name is the anchor used by the visibility rewriter.
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

        if (method.getAnnotatedQuery() == null || !method.getRequiredDeclaredQuery().isNative()) {
            return applicationRewriter;
        }

        String visibilityParameter = visibilityParameter(method);
        if (visibilityParameter == null) {
            return applicationRewriter;
        }

        QueryRewriter platformRewriter =
                (query, sort) -> visibilityRewriter.rewrite(query, sort, visibilityParameter);

        if (method.getQueryRewriter() == QueryRewriter.IdentityQueryRewriter.class) {
            return platformRewriter;
        }

        return (query, sort) ->
                platformRewriter.rewrite(applicationRewriter.rewrite(query, sort), sort);
    }

    private String visibilityParameter(JpaQueryMethod queryMethod) {
        String result = null;

        for (var parameter : queryMethod.getParameters()) {
            Optional<String> name = parameter.getName();

            if (parameter.getParameter().getAnnotation(ResourceVisibilityGroups.class) == null) {
                continue;
            }
            if (result != null) {
                throw new ResourceVisibilityNativeQueryException(
                        "Native visibility query must declare exactly one @ResourceVisibilityGroups parameter"
                );
            }
            if (name.isEmpty() || name.get().isBlank()) {
                throw new ResourceVisibilityNativeQueryException(
                        "@ResourceVisibilityGroups must also declare a named @Param"
                );
            }
            result = name.get();
        }

        return result;
    }
}

