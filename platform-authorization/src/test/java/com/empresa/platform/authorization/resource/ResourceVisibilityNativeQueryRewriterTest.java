package com.empresa.platform.authorization.resource;

import org.junit.jupiter.api.Test;
import org.springframework.data.domain.Sort;

import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

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
    void shouldApplyLegacyStrategyInsideVisibilityScope() {
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
    }

    @Test
    void normalUserShouldKeepMandatoryAndPredicateUntouched() {
        NativeResourceVisibilityContext context = new NativeResourceVisibilityContext();
        ResourceVisibilityNativeQueryRewriter rewriter = rewriter(context);
        String sql = """
                SELECT app.id
                FROM applications app
                WHERE app.account_id = :accountId
                  AND LOWER(app.authorizer_group) IN (:authorizerGroups)
                  AND app.active = true
                """;

        context.enter(false);
        try {
            assertThat(rewriter.rewrite(sql, Sort.unsorted(), "authorizerGroups")).isEqualTo(sql);
        } finally {
            context.exit();
        }
    }

    @Test
    void ownerShouldWrapMandatoryAndPredicate() {
        NativeResourceVisibilityContext context = new NativeResourceVisibilityContext();
        ResourceVisibilityNativeQueryRewriter rewriter = rewriter(context);
        String sql = """
                SELECT app.id
                FROM applications app
                WHERE app.account_id = :accountId
                  AND LOWER(app.authorizer_group) IN (:authorizerGroups)
                  AND app.active = true
                """;

        context.enter(true);
        try {
            String rewritten = rewriter.rewrite(sql, Sort.unsorted(), "authorizerGroups");

            assertThat(rewritten)
                    .contains("AND (TRUE = TRUE OR LOWER(app.authorizer_group) IN (:authorizerGroups))")
                    .contains("AND app.active = true");
        } finally {
            context.exit();
        }
    }

    @Test
    void ownerShouldWrapPredicateImmediatelyAfterWhere() {
        NativeResourceVisibilityContext context = new NativeResourceVisibilityContext();
        ResourceVisibilityNativeQueryRewriter rewriter = rewriter(context);
        String sql = """
                SELECT app.id
                FROM applications app
                WHERE LOWER(app.authorizer_group) IN (:authorizerGroups)
                  AND app.active = true
                """;

        context.enter(true);
        try {
            assertThat(rewriter.rewrite(sql, Sort.unsorted(), "authorizerGroups"))
                    .contains("WHERE (TRUE = TRUE OR LOWER(app.authorizer_group) IN (:authorizerGroups))");
        } finally {
            context.exit();
        }
    }

    @Test
    void shouldSupportColumnPredicateWithoutLower() {
        NativeResourceVisibilityContext context = new NativeResourceVisibilityContext();
        ResourceVisibilityNativeQueryRewriter rewriter = rewriter(context);
        String sql = "SELECT * FROM applications app WHERE app.authorizer_group IN (:groups)";

        context.enter(true);
        try {
            assertThat(rewriter.rewrite(sql, Sort.unsorted(), "groups"))
                    .isEqualTo("SELECT * FROM applications app WHERE (TRUE = TRUE OR app.authorizer_group IN (:groups))");
        } finally {
            context.exit();
        }
    }

    @Test
    void shouldFailClosedWhenVisibilityPredicateIsUnderOr() {
        NativeResourceVisibilityContext context = new NativeResourceVisibilityContext();
        ResourceVisibilityNativeQueryRewriter rewriter = rewriter(context);
        String sql = """
                SELECT *
                FROM applications app
                WHERE app.active = true
                   OR LOWER(app.authorizer_group) IN (:authorizerGroups)
                """;

        context.enter(false);
        try {
            assertThatThrownBy(() -> rewriter.rewrite(sql, Sort.unsorted(), "authorizerGroups"))
                    .isInstanceOf(ResourceVisibilityNativeQueryException.class)
                    .hasMessageContaining("mandatory AND-only IN predicate");
        } finally {
            context.exit();
        }
    }

    @Test
    void shouldFailClosedWhenVisibilityPredicateIsMissing() {
        NativeResourceVisibilityContext context = new NativeResourceVisibilityContext();
        ResourceVisibilityNativeQueryRewriter rewriter = rewriter(context);

        context.enter(false);
        try {
            assertThatThrownBy(() -> rewriter.rewrite(
                    "SELECT * FROM applications WHERE active = true",
                    Sort.unsorted(),
                    "authorizerGroups"))
                    .isInstanceOf(ResourceVisibilityNativeQueryException.class);
        } finally {
            context.exit();
        }
    }

    @Test
    void shouldFailClosedWhenVisibilityPredicateOccursMoreThanOnce() {
        NativeResourceVisibilityContext context = new NativeResourceVisibilityContext();
        ResourceVisibilityNativeQueryRewriter rewriter = rewriter(context);
        String sql = """
                SELECT *
                FROM applications app
                WHERE app.authorizer_group IN (:authorizerGroups)
                  AND app.fallback_group IN (:authorizerGroups)
                """;

        context.enter(false);
        try {
            assertThatThrownBy(() -> rewriter.rewrite(sql, Sort.unsorted(), "authorizerGroups"))
                    .isInstanceOf(ResourceVisibilityNativeQueryException.class)
                    .hasMessageContaining("exactly once");
        } finally {
            context.exit();
        }
    }

    @Test
    void unrelatedOrShouldNotPreventMandatoryVisibilityPredicate() {
        NativeResourceVisibilityContext context = new NativeResourceVisibilityContext();
        ResourceVisibilityNativeQueryRewriter rewriter = rewriter(context);
        String sql = """
                SELECT *
                FROM applications app
                WHERE (app.active = true OR app.is_default = true)
                  AND app.authorizer_group IN (:authorizerGroups)
                """;

        context.enter(true);
        try {
            String rewritten = rewriter.rewrite(sql, Sort.unsorted(), "authorizerGroups");
            assertThat(rewritten)
                    .contains("(app.active = true OR app.is_default = true)")
                    .contains("AND (TRUE = TRUE OR app.authorizer_group IN (:authorizerGroups))");
        } finally {
            context.exit();
        }
    }


    @Test
    void shouldFailClosedWhenVisibilityIsInsideParenthesizedOrAfterAnd() {
        NativeResourceVisibilityContext context = new NativeResourceVisibilityContext();
        ResourceVisibilityNativeQueryRewriter rewriter = rewriter(context);
        String sql = """
                SELECT *
                FROM applications app
                WHERE app.account_id = :accountId
                  AND (app.active = true
                       OR LOWER(app.authorizer_group) IN (:authorizerGroups))
                """;

        context.enter(false);
        try {
            assertThatThrownBy(() -> rewriter.rewrite(sql, Sort.unsorted(), "authorizerGroups"))
                    .isInstanceOf(ResourceVisibilityNativeQueryException.class);
        } finally {
            context.exit();
        }
    }

    @Test
    void shouldFailClosedWhenOrAppearsAfterVisibilityPredicate() {
        NativeResourceVisibilityContext context = new NativeResourceVisibilityContext();
        ResourceVisibilityNativeQueryRewriter rewriter = rewriter(context);
        String sql = """
                SELECT *
                FROM applications app
                WHERE app.account_id = :accountId
                  AND LOWER(app.authorizer_group) IN (:authorizerGroups)
                   OR app.is_public = true
                """;

        context.enter(false);
        try {
            assertThatThrownBy(() -> rewriter.rewrite(sql, Sort.unsorted(), "authorizerGroups"))
                    .isInstanceOf(ResourceVisibilityNativeQueryException.class);
        } finally {
            context.exit();
        }
    }

    @Test
    void shouldFailClosedWhenParameterAppearsOnlyInCommentOrLiteral() {
        NativeResourceVisibilityContext context = new NativeResourceVisibilityContext();
        ResourceVisibilityNativeQueryRewriter rewriter = rewriter(context);

        context.enter(false);
        try {
            assertThatThrownBy(() -> rewriter.rewrite(
                    "SELECT ':authorizerGroups' AS marker FROM applications app WHERE app.active = true /* LOWER(app.authorizer_group) IN (:authorizerGroups) */",
                    Sort.unsorted(),
                    "authorizerGroups"))
                    .isInstanceOf(ResourceVisibilityNativeQueryException.class);
        } finally {
            context.exit();
        }
    }

    @Test
    void shouldFailClosedForNotInVisibilityPredicate() {
        NativeResourceVisibilityContext context = new NativeResourceVisibilityContext();
        ResourceVisibilityNativeQueryRewriter rewriter = rewriter(context);

        context.enter(false);
        try {
            assertThatThrownBy(() -> rewriter.rewrite(
                    "SELECT * FROM applications app WHERE app.authorizer_group NOT IN (:authorizerGroups)",
                    Sort.unsorted(),
                    "authorizerGroups"))
                    .isInstanceOf(ResourceVisibilityNativeQueryException.class);
        } finally {
            context.exit();
        }
    }

    @Test
    void nestedScopeShouldRemainActiveUntilOuterScopeExits() {
        NativeResourceVisibilityContext context = new NativeResourceVisibilityContext();

        context.enter(false);
        context.enter(false);
        context.exit();

        assertThat(context.isActive()).isTrue();
        assertThat(context.isOwner()).isFalse();

        context.exit();

        assertThat(context.isActive()).isFalse();
    }

    @Test
    void nestedOwnerScopeShouldRetainOwnerUntilOuterScopeExits() {
        NativeResourceVisibilityContext context = new NativeResourceVisibilityContext();

        context.enter(false);
        context.enter(true);
        context.exit();

        assertThat(context.isActive()).isTrue();
        assertThat(context.isOwner()).isTrue();

        context.exit();
        assertThat(context.isActive()).isFalse();
    }

    private ResourceVisibilityNativeQueryRewriter rewriter(NativeResourceVisibilityContext context) {
        return new ResourceVisibilityNativeQueryRewriter(context, sql -> sql);
    }
}
