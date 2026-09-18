package com.empresa.platform.authorization.resource;

import org.junit.jupiter.api.Test;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.repository.QueryRewriter;
import org.springframework.data.jpa.repository.query.JpaQueryMethod;
import org.springframework.data.jpa.repository.query.QueryRewriterProvider;
import org.springframework.data.jpa.repository.query.DeclaredQuery;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.doReturn;

class ResourceVisibilityQueryRewriterProviderTest {

    @Test
    void shouldComposeCustomNativeRewriterBeforeVisibility() {
        NativeResourceVisibilityContext context = new NativeResourceVisibilityContext();
        JpaQueryMethod method = queryMethod(true, CustomQueryRewriter.class);
        QueryRewriter custom = (query, sort) -> query + " custom";
        ResourceVisibilityQueryRewriterProvider provider = provider(ignored -> custom, context);

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
        NativeResourceVisibilityContext context = new NativeResourceVisibilityContext();
        JpaQueryMethod method = queryMethod(true, QueryRewriter.IdentityQueryRewriter.class);
        ResourceVisibilityQueryRewriterProvider provider =
                provider(ignored -> QueryRewriter.IdentityQueryRewriter.INSTANCE, context);

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
        JpaQueryMethod method = queryMethod(false, QueryRewriter.IdentityQueryRewriter.class);
        QueryRewriter delegated = (sql, sort) -> sql + " delegated";
        ResourceVisibilityQueryRewriterProvider provider =
                provider(ignored -> delegated, new NativeResourceVisibilityContext());

        assertThat(provider.getQueryRewriter(method).rewrite("SELECT 1", Sort.unsorted()))
                .isEqualTo("SELECT 1 delegated");
    }

    @Test
    void shouldLeaveUndeclaredQueryWithDelegateWithoutInspectingDeclaredQuery() {
        JpaQueryMethod method = mock(JpaQueryMethod.class);
        when(method.getAnnotatedQuery()).thenReturn(null);
        QueryRewriter delegated = (sql, sort) -> sql + " delegated";
        ResourceVisibilityQueryRewriterProvider provider =
                provider(ignored -> delegated, new NativeResourceVisibilityContext());

        assertThat(provider.getQueryRewriter(method).rewrite("SELECT 1", Sort.unsorted()))
                .isEqualTo("SELECT 1 delegated");
    }

    private JpaQueryMethod queryMethod(boolean nativeQuery, Class<? extends QueryRewriter> rewriterType) {
        JpaQueryMethod method = mock(JpaQueryMethod.class);
        DeclaredQuery declaredQuery = mock(DeclaredQuery.class);
        when(declaredQuery.isNative()).thenReturn(nativeQuery);
        when(method.getAnnotatedQuery()).thenReturn("SELECT 1");
        when(method.getRequiredDeclaredQuery()).thenReturn(declaredQuery);
        doReturn(rewriterType).when(method).getQueryRewriter();
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
