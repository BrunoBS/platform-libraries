package com.empresa.platform.authorization.resource;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class JSqlParserNativeResourceVisibilityStrategyTest {

    private JSqlParserNativeResourceVisibilityStrategy strategy;

    @BeforeEach
    void setUp() {
        strategy = new JSqlParserNativeResourceVisibilityStrategy(
                new ResourceVisibilityMetadataRegistry(
                        java.util.List.of(
                                new ResourceVisibilityMetadata("accounts", "authorizer_group"),
                                new ResourceVisibilityMetadata("applications", "authorizer_group")
                        )
                )
        );
    }

    @Test
    void shouldInjectVisibilityPredicateWithoutRequiringColumnInProjection() {
        String rewritten = strategy.apply("SELECT a.id, a.name FROM accounts a");

        assertThat(rewritten)
                .containsIgnoringCase("SELECT a.id, a.name FROM accounts a")
                .containsIgnoringCase("LOWER(a.authorizer_group)")
                .containsIgnoringCase("JSON_CONTAINS")
                .contains("@platform_resource_visibility_authorizers");
    }

    @Test
    void shouldPreserveExistingWhereAndAppendVisibilityPredicate() {
        String rewritten = strategy.apply("SELECT a.id FROM accounts a WHERE a.deleted_at IS NULL");

        assertThat(rewritten)
                .containsIgnoringCase("a.deleted_at IS NULL")
                .containsIgnoringCase("LOWER(a.authorizer_group)")
                .containsIgnoringCase(" AND ");
    }

    @Test
    void shouldUseTableNameWhenAliasIsAbsent() {
        String rewritten = strategy.apply("SELECT accounts.id FROM accounts");

        assertThat(rewritten).containsIgnoringCase("LOWER(accounts.authorizer_group)");
    }

    @Test
    void shouldFailClosedWhenOnlyJoinedTableIsProtected() {
        assertThatThrownBy(() -> strategy.apply(
                "SELECT x.id FROM auxiliary x JOIN accounts a ON a.id = x.account_id"
        ))
                .isInstanceOf(ResourceVisibilityNativeQueryException.class)
                .hasMessageContaining("root resource");
    }

    @Test
    void shouldProtectOnlyRootResourceWhenJoinedTableIsAlsoRegistered() {
        String rewritten = strategy.apply(
                "SELECT a.id, app.id FROM accounts a LEFT JOIN applications app ON app.account_id = a.id"
        );

        assertThat(rewritten)
                .containsIgnoringCase("LOWER(a.authorizer_group)")
                .doesNotContainIgnoringCase("LOWER(app.authorizer_group)");
    }

    @Test
    void shouldProtectOnlyRootResourceWithInnerJoinToAnotherRegisteredResource() {
        String rewritten = strategy.apply(
                "SELECT a.id, app.id FROM accounts a INNER JOIN applications app ON app.account_id = a.id"
        );

        assertThat(rewritten)
                .containsIgnoringCase("LOWER(a.authorizer_group)")
                .doesNotContainIgnoringCase("LOWER(app.authorizer_group)")
                .containsIgnoringCase("INNER JOIN applications app");
    }

    @Test
    void shouldPreserveParametersOrderByAndLimit() {
        String rewritten = strategy.apply(
                "SELECT a.id FROM accounts a WHERE a.name = :name ORDER BY a.id DESC LIMIT 10"
        );

        assertThat(rewritten)
                .contains(":name")
                .containsIgnoringCase("LOWER(a.authorizer_group)")
                .containsIgnoringCase("ORDER BY a.id DESC")
                .containsIgnoringCase("LIMIT 10");
    }

    @Test
    void shouldGenerateExplicitTruthyJsonContainsPredicate() {
        String rewritten = strategy.apply("SELECT a.id FROM accounts a");

        assertThat(rewritten)
                .containsIgnoringCase("JSON_CONTAINS")
                .contains("= 1")
                .doesNotContain("1 <= JSON_CONTAINS");
    }

    @Test
    void shouldPreserveGroupByHavingOrderByAndLimit() {
        String rewritten = strategy.apply(
                "SELECT a.authorizer_group, COUNT(*) total FROM accounts a " +
                        "GROUP BY a.authorizer_group HAVING COUNT(*) > 0 ORDER BY total DESC LIMIT 5"
        );

        assertThat(rewritten)
                .containsIgnoringCase("LOWER(a.authorizer_group)")
                .containsIgnoringCase("GROUP BY a.authorizer_group")
                .containsIgnoringCase("HAVING COUNT(*) > 0")
                .containsIgnoringCase("ORDER BY total DESC")
                .containsIgnoringCase("LIMIT 5");
    }

    @Test
    void shouldProtectOnlyRootAliasInSelfJoin() {
        String rewritten = strategy.apply(
                "SELECT parent.id, child.id FROM accounts parent JOIN accounts child ON child.id = parent.id"
        );

        assertThat(rewritten)
                .containsIgnoringCase("LOWER(parent.authorizer_group)")
                .doesNotContainIgnoringCase("LOWER(child.authorizer_group)");
    }

    @Test
    void shouldFailClosedForUnionUntilExplicitlySupported() {
        assertThatThrownBy(() -> strategy.apply(
                "SELECT a.id FROM accounts a UNION SELECT app.id FROM applications app"
        ))
                .isInstanceOf(ResourceVisibilityNativeQueryException.class)
                .hasMessageContaining("Only simple SELECT");
    }

    @Test
    void shouldFailClosedForCteUntilExplicitlySupported() {
        assertThatThrownBy(() -> strategy.apply(
                "WITH visible AS (SELECT a.id FROM accounts a) SELECT visible.id FROM visible"
        ))
                .isInstanceOf(ResourceVisibilityNativeQueryException.class);
    }

    @Test
    void shouldFailClosedWhenNoProtectedResourceIsPresent() {
        assertThatThrownBy(() -> strategy.apply("SELECT x.id FROM auxiliary x"))
                .isInstanceOf(ResourceVisibilityNativeQueryException.class)
                .hasMessageContaining("root resource");
    }

    @Test
    void shouldFailClosedForBlankSql() {
        assertThatThrownBy(() -> strategy.apply(" "))
                .isInstanceOf(ResourceVisibilityNativeQueryException.class)
                .hasMessageContaining("must not be empty");
    }

    @Test
    void shouldFailClosedForNonSelectStatement() {
        assertThatThrownBy(() -> strategy.apply("UPDATE accounts SET name = 'x'"))
                .isInstanceOf(ResourceVisibilityNativeQueryException.class)
                .hasMessageContaining("Only simple SELECT");
    }

    @Test
    void shouldFailClosedForSubqueryFromItemUntilExplicitlySupported() {
        assertThatThrownBy(() -> strategy.apply("SELECT x.id FROM (SELECT id FROM accounts) x"))
                .isInstanceOf(ResourceVisibilityNativeQueryException.class)
                .hasMessageContaining("Subqueries");
    }
}
