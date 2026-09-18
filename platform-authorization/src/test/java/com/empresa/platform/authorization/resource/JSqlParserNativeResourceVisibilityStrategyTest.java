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
                        java.util.List.of(new ResourceVisibilityMetadata("accounts", "authorizer_group"))
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
    void shouldProtectRegisteredTableInJoin() {
        String rewritten = strategy.apply("SELECT x.id FROM auxiliary x JOIN accounts a ON a.id = x.account_id");

        assertThat(rewritten).containsIgnoringCase("LOWER(a.authorizer_group)");
    }

    @Test
    void shouldFailClosedWhenNoProtectedResourceIsPresent() {
        assertThatThrownBy(() -> strategy.apply("SELECT x.id FROM auxiliary x"))
                .isInstanceOf(ResourceVisibilityNativeQueryException.class)
                .hasMessageContaining("registered visibility resource");
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
