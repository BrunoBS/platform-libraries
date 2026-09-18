package com.empresa.platform.authorization.resource;

import com.empresa.platform.authorization.annotation.ResourceVisibilityGroups;
import org.springframework.data.jpa.repository.QueryRewriter;
import org.springframework.data.jpa.repository.query.JpaQueryMethod;
import org.springframework.data.jpa.repository.query.QueryRewriterProvider;
import org.springframework.data.repository.query.Param;

import java.lang.annotation.Annotation;
import java.lang.reflect.Method;

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

        String visibilityParameter = visibilityParameter(method.getMethod());
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

    private String visibilityParameter(Method method) {
        Annotation[][] annotations = method.getParameterAnnotations();
        String result = null;

        for (Annotation[] parameterAnnotations : annotations) {
            ResourceVisibilityGroups visibility = null;
            Param param = null;

            for (Annotation annotation : parameterAnnotations) {
                if (annotation instanceof ResourceVisibilityGroups resourceVisibilityGroups) {
                    visibility = resourceVisibilityGroups;
                } else if (annotation instanceof Param repositoryParam) {
                    param = repositoryParam;
                }
            }

            if (visibility == null) {
                continue;
            }
            if (result != null) {
                throw new ResourceVisibilityNativeQueryException(
                        "Native visibility query must declare exactly one @ResourceVisibilityGroups parameter"
                );
            }
            if (param == null || param.value().isBlank()) {
                throw new ResourceVisibilityNativeQueryException(
                        "@ResourceVisibilityGroups must also declare a named @Param"
                );
            }
            result = param.value();
        }

        return result;
    }
}
