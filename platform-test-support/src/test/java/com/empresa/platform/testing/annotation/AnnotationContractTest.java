package com.empresa.platform.testing.annotation;

import com.empresa.platform.testing.database.CleanupMode;
import com.empresa.platform.testing.authorization.AuthorizationMockResult;
import org.junit.jupiter.api.Test;
import org.junit.platform.commons.support.AnnotationSupport;

import static org.assertj.core.api.Assertions.assertThat;

class AnnotationContractTest {

    @WithMySql
    static class DefaultMySqlTest {
    }

    @WithMySql(cleanup = CleanupMode.NONE, excludeTables = {"catalog"})
    static class CustomMySqlTest {
    }

    @WithMockAuthorization
    static class DefaultAuthorizationTest {
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
}
