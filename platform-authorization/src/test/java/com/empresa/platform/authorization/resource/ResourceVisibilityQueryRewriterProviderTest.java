package com.empresa.platform.authorization.resource;

import org.junit.jupiter.api.Test;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.repository.QueryRewriter;
import org.springframework.data.jpa.repository.query.JpaQueryMethod;
import org.springframework.data.jpa.repository.query.QueryRewriterProvider;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class ResourceVisibilityQueryRewriterProviderTest {

    @Test
    void shouldComposeCustomNativeRewriterBeforeVisibility() {
        JpaQueryMethod method = mock(JpaQueryMethod.class);
        QueryRewriter custom = (query, sort) -> query + " custom";
        QueryRewriterProvider delegate = ignored -> custom;

        when(method.getAnnotatedQuery()).thenReturn("SELECT a.id FROM accounts a");
        when(method.getRequiredDeclaredQuery()).thenReturn(
                new org.springframework.data.repository.query.DeclaredQuery() {
                    @Override public boolean hasNamedQueryName() { return false; }
                    @Override public String getNamedQueryName() { return null; }
                    @Override public String getQueryString() { return "SELECT a.id FROM accounts a"; }
                    @Override public boolean isNative() { return true; }
                });
        when(method.getQueryRewriter()).thenReturn(CustomQueryRewriter.class);

        NativeResourceVisibilityContext context = new NativeResourceVisibilityContext();
        ResourceVisibilityNativeQueryRewriter visibility =
                new ResourceVisibilityNativeQueryRewriter(context, sql -> sql + " visibility");
        ResourceVisibilityQueryRewriterProvider provider =
                new ResourceVisibilityQueryRewriterProvider(delegate, visibility);

        context.enter();
        try {
            assertThat(provider.getQueryRewriter(method).rewrite("SELECT 1", Sort.unsorted()))
                    .isEqualTo("SELECT 1 custom visibility");
        } finally {
            context.exit();
        }
    }

    static class CustomQueryRewriter implements QueryRewriter {
        @Override
        public String rewrite(String query, Sort sort) {
            return query;
        }
    }
}
