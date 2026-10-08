package br.com.portalmanager.platform.library.testing.database;

import br.com.portalmanager.platform.library.testing.database.annotation.WithDatabaseScripts;
import br.com.portalmanager.platform.library.testing.database.annotation.WithMySql;
import org.junit.jupiter.api.Test;
import org.junit.platform.commons.support.AnnotationSupport;

import java.lang.reflect.Method;

import static org.assertj.core.api.Assertions.assertThat;

class DatabaseAnnotationContractTest {

    @WithMySql
    static class DefaultMySqlTest {
    }

    @WithMySql(cleanup = CleanupMode.NONE, excludeTables = {"catalog"})
    static class CustomMySqlTest {
    }

    @WithDatabaseScripts(setup = "classpath:sql/create-view.sql", cleanup = "classpath:sql/drop-view.sql")
    static class DefaultScriptsTest {
    }

    static class RepeatableScriptsTest {
        @WithDatabaseScripts(setup = "classpath:sql/first.sql")
        @WithDatabaseScripts(setup = "classpath:sql/second.sql")
        void scenario() {
        }
    }

    @Test
    void shouldExposeDefaultAndCustomMySqlCleanupOptions() {
        var defaults = AnnotationSupport.findAnnotation(DefaultMySqlTest.class, WithMySql.class).orElseThrow();
        assertThat(defaults.cleanup()).isEqualTo(CleanupMode.BEFORE_EACH);
        assertThat(defaults.excludeTables()).containsExactly("flyway_schema_history");

        var custom = AnnotationSupport.findAnnotation(CustomMySqlTest.class, WithMySql.class).orElseThrow();
        assertThat(custom.cleanup()).isEqualTo(CleanupMode.NONE);
        assertThat(custom.excludeTables()).containsExactly("catalog");
    }

    @Test
    void shouldExposeDatabaseScriptDefaultsAndRepeatableMethodDeclarations() throws NoSuchMethodException {
        var defaults = AnnotationSupport.findAnnotation(DefaultScriptsTest.class, WithDatabaseScripts.class).orElseThrow();
        assertThat(defaults.setup()).containsExactly("classpath:sql/create-view.sql");
        assertThat(defaults.cleanup()).containsExactly("classpath:sql/drop-view.sql");
        assertThat(defaults.continueOnError()).isFalse();
        assertThat(defaults.setupPhase()).isEqualTo(DatabaseSetupPhase.BEFORE_TEST_CLASS);
        assertThat(defaults.cleanupPhase()).isEqualTo(DatabaseCleanupPhase.AFTER_TEST_CLASS);

        Method method = RepeatableScriptsTest.class.getDeclaredMethod("scenario");
        var scripts = AnnotationSupport.findRepeatableAnnotations(method, WithDatabaseScripts.class);
        assertThat(scripts).hasSize(2);
        assertThat(scripts.getFirst().setup()).containsExactly("classpath:sql/first.sql");
        assertThat(scripts.getLast().setup()).containsExactly("classpath:sql/second.sql");
    }
}
