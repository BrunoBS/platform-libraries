package br.com.portalmanager.platform.library.testing.contract;

import br.com.portalmanager.platform.library.testing.database.annotation.WithDatabaseScripts;
import br.com.portalmanager.platform.library.testing.database.annotation.WithMySql;
import br.com.portalmanager.platform.library.testing.authorization.annotation.WithMockAuthorization;
import br.com.portalmanager.platform.library.testing.lifecycle.annotation.PlatformUnitTest;
import br.com.portalmanager.platform.library.testing.lifecycle.annotation.PlatformIntegrationTest;
import br.com.portalmanager.platform.library.testing.database.CleanupMode;
import br.com.portalmanager.platform.library.testing.database.DatabaseCleanupPhase;
import br.com.portalmanager.platform.library.testing.database.DatabaseSetupPhase;
import br.com.portalmanager.platform.library.testing.authorization.AuthorizationMockResult;
import org.junit.jupiter.api.Test;
import org.junit.platform.commons.support.AnnotationSupport;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.core.annotation.AnnotatedElementUtils;

import java.lang.reflect.Method;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class AnnotationContractTest {

    @PlatformIntegrationTest
    static class DefaultIntegrationTest {
    }

    @PlatformIntegrationTest(webEnvironment = SpringBootTest.WebEnvironment.NONE)
    static class NonWebIntegrationTest {
    }

    @WithMySql
    static class DefaultMySqlTest {
    }

    @WithMySql(cleanup = CleanupMode.NONE, excludeTables = {"catalog"})
    static class CustomMySqlTest {
    }

    @WithMockAuthorization
    static class DefaultAuthorizationTest {
    }

    @PlatformUnitTest
    static class DefaultUnitTest {
    }

    @WithDatabaseScripts(
            setup = "classpath:sql/create-view.sql",
            cleanup = "classpath:sql/drop-view.sql"
    )
    static class DefaultDatabaseScriptsTest {
    }

    static class MethodDatabaseScriptsTest {

        @WithDatabaseScripts(setup = "classpath:sql/first.sql")
        @WithDatabaseScripts(setup = "classpath:sql/second.sql")
        void scenario() {
        }
    }

    @Test
    void shouldKeepRandomPortAsTheDefaultIntegrationEnvironment() {
        SpringBootTest annotation = AnnotatedElementUtils
                .findMergedAnnotation(DefaultIntegrationTest.class, SpringBootTest.class);

        assertThat(annotation).isNotNull();
        assertThat(annotation.webEnvironment()).isEqualTo(SpringBootTest.WebEnvironment.RANDOM_PORT);
    }

    @Test
    void shouldAllowIntegrationTestsWithoutAnHttpServer() {
        SpringBootTest annotation = AnnotatedElementUtils
                .findMergedAnnotation(NonWebIntegrationTest.class, SpringBootTest.class);

        assertThat(annotation).isNotNull();
        assertThat(annotation.webEnvironment()).isEqualTo(SpringBootTest.WebEnvironment.NONE);
    }

    @Test
    void shouldExposeDefaultMySqlCleanupConfiguration() {
        WithMySql annotation = AnnotationSupport.findAnnotation(DefaultMySqlTest.class, WithMySql.class).orElseThrow();

        assertThat(annotation.cleanup()).isEqualTo(CleanupMode.BEFORE_EACH);
        assertThat(annotation.excludeTables()).containsExactly("flyway_schema_history");
    }

    @Test
    void shouldExposeCustomMySqlCleanupConfiguration() {
        WithMySql annotation = AnnotationSupport.findAnnotation(CustomMySqlTest.class, WithMySql.class).orElseThrow();

        assertThat(annotation.cleanup()).isEqualTo(CleanupMode.NONE);
        assertThat(annotation.excludeTables()).containsExactly("catalog");
    }

    @Test
    void shouldAllowAuthorizationByDefault() {
        WithMockAuthorization annotation = AnnotationSupport
                .findAnnotation(DefaultAuthorizationTest.class, WithMockAuthorization.class)
                .orElseThrow();

        assertThat(annotation.defaultResult()).isEqualTo(AuthorizationMockResult.ALLOWED);
    }

    @Test
    void shouldExposePlatformUnitTestAnnotation() {
        assertThat(AnnotationSupport.findAnnotation(DefaultUnitTest.class, PlatformUnitTest.class))
                .isPresent();
    }

    @Test
    void shouldExposeDatabaseScriptLifecycle() {
        WithDatabaseScripts annotation = AnnotationSupport
                .findAnnotation(DefaultDatabaseScriptsTest.class, WithDatabaseScripts.class)
                .orElseThrow();

        assertThat(annotation.setup()).containsExactly("classpath:sql/create-view.sql");
        assertThat(annotation.cleanup()).containsExactly("classpath:sql/drop-view.sql");
        assertThat(annotation.setupPhase()).isEqualTo(DatabaseSetupPhase.BEFORE_TEST_CLASS);
        assertThat(annotation.cleanupPhase()).isEqualTo(DatabaseCleanupPhase.AFTER_TEST_CLASS);
        assertThat(annotation.continueOnError()).isFalse();
    }

    @Test
    void shouldAllowRepeatableScriptsOnTestMethod() throws NoSuchMethodException {
        Method method = MethodDatabaseScriptsTest.class.getDeclaredMethod("scenario");
        List<WithDatabaseScripts> annotations = AnnotationSupport.findRepeatableAnnotations(
                method,
                WithDatabaseScripts.class
        );

        assertThat(annotations).hasSize(2);
        assertThat(annotations.getFirst().setup()).containsExactly("classpath:sql/first.sql");
        assertThat(annotations.getLast().setup()).containsExactly("classpath:sql/second.sql");
    }
}
