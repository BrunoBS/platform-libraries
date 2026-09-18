package com.empresa.platform.authorization.resource;

import org.junit.jupiter.api.Test;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.jpa.repository.QueryRewriter;
import org.springframework.data.jpa.repository.query.JpaQueryMethod;
import org.springframework.data.jpa.repository.query.QueryRewriterProvider;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class ResourceVisibilityQueryRewriterProviderTest {

    @Test
    void shouldComposeCustomNativeRewriterBeforeVisibility() {
        JpaQueryMethod method = nativeMethod(CustomQueryRewriter.class);
        QueryRewriter custom = (query, sort) -> query + " custom";
        QueryRewriterProvider delegate = ignored -> custom;
        NativeResourceVisibilityContext context = new NativeResourceVisibilityContext();
        ResourceVisibilityQueryRewriterProvider provider = provider(delegate, context);
        context.enter();
        try {
            assertThat(provider.getQueryRewriter(method).rewrite("SELECT 1", Sort.unsorted()))
                    .isEqualTo("SELECT 1 custom visibility");
        } finally {
            context.exit();
        }
    }

    @Test
    void shouldUseVisibilityRewriterForDefaultNativeQuery() {
        JpaQueryMethod method = nativeMethod(QueryRewriter.IdentityQueryRewriter.class);
        QueryRewriterProvider delegate = ignored -> QueryRewriter.IdentityQueryRewriter.INSTANCE;
        NativeResourceVisibilityContext context = new NativeResourceVisibilityContext();
        ResourceVisibilityNativeQueryRewriter visibility =
                new ResourceVisibilityNativeQueryRewriter(context, sql -> sql + " visibility");
        ResourceVisibilityQueryRewriterProvider provider =
                new ResourceVisibilityQueryRewriterProvider(delegate, visibility);

        context.enter();
        try {
            assertThat(provider.getQueryRewriter(method).rewrite("SELECT 1", Sort.unsorted()))
                    .isEqualTo("SELECT 1 visibility");
        } finally {
            context.exit();
        }
    }

    @Test
    void shouldLeaveNonNativeQueryWithDelegate() {
        JpaQueryMethod method = mock(JpaQueryMethod.class);
        Query query = mock(Query.class);
        when(query.nativeQuery()).thenReturn(false);
        when(method.getAnnotatedQuery()).thenReturn("SELECT a FROM Account a");
        when(method.getQueryAnnotation()).thenReturn(query);

        QueryRewriter delegated = (sql, sort) -> sql + " delegated";
        ResourceVisibilityQueryRewriterProvider provider =
                provider(ignored -> delegated, new NativeResourceVisibilityContext());

        assertThat(provider.getQueryRewriter(method).rewrite("SELECT 1", Sort.unsorted()))
                .isEqualTo("SELECT 1 delegated");
    }

    private JpaQueryMethod nativeMethod(Class<? extends QueryRewriter> rewriterType) {
        JpaQueryMethod method = mock(JpaQueryMethod.class);
        Query query = mock(Query.class);
        when(query.nativeQuery()).thenReturn(true);
        when(method.getAnnotatedQuery()).thenReturn("SELECT a.id FROM accounts a");
        when(method.getQueryAnnotation()).thenReturn(query);
        when(method.getQueryRewriter()).thenReturn(rewriterType);
        return method;
    }

    private ResourceVisibilityQueryRewriterProvider provider(
            QueryRewriterProvider delegate,
            NativeResourceVisibilityContext context) {
        ResourceVisibilityNativeQueryRewriter visibility =
                new ResourceVisibilityNativeQueryRewriter(context, sql -> sql + " visibility");
        return new ResourceVisibilityQueryRewriterProvider(delegate, visibility);
    }

    static class CustomQueryRewriter implements QueryRewriter {
        @Override
        public String rewrite(String query, Sort sort) {
            return query;
        }
    }
}
