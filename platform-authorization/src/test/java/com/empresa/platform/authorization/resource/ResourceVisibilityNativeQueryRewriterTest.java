package com.empresa.platform.authorization.resource;

import org.junit.jupiter.api.Test;
import org.springframework.data.domain.Sort;

import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;

class ResourceVisibilityNativeQueryRewriterTest {

    @Test
    void shouldLeaveQueryUntouchedOutsideVisibilityScope() {
        NativeResourceVisibilityContext context = new NativeResourceVisibilityContext();
        AtomicInteger calls = new AtomicInteger();
        NativeResourceVisibilityStrategy strategy = sql -> {
            calls.incrementAndGet();
            return sql + " protected";
        };
        ResourceVisibilityNativeQueryRewriter rewriter =
                new ResourceVisibilityNativeQueryRewriter(context, strategy);

        assertThat(rewriter.rewrite("SELECT 1", Sort.unsorted())).isEqualTo("SELECT 1");
        assertThat(calls).hasValue(0);
    }

    @Test
    void shouldApplyStrategyInsideVisibilityScope() {
        NativeResourceVisibilityContext context = new NativeResourceVisibilityContext();
        ResourceVisibilityNativeQueryRewriter rewriter =
                new ResourceVisibilityNativeQueryRewriter(context, sql -> sql + " protected");

        context.enter();
        try {
            assertThat(rewriter.rewrite("SELECT 1", Sort.unsorted()))
                    .isEqualTo("SELECT 1 protected");
        } finally {
            context.exit();
        }

        assertThat(context.isActive()).isFalse();
    }

    @Test
    void nestedScopeShouldRemainActiveUntilOuterScopeExits() {
        NativeResourceVisibilityContext context = new NativeResourceVisibilityContext();

        context.enter();
        context.enter();
        context.exit();

        assertThat(context.isActive()).isTrue();

        context.exit();

        assertThat(context.isActive()).isFalse();
    }
}
