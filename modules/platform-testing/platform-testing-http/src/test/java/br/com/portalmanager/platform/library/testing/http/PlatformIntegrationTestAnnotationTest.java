package br.com.portalmanager.platform.library.testing.http;

import br.com.portalmanager.platform.library.testing.lifecycle.annotation.PlatformIntegrationTest;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.core.annotation.AnnotatedElementUtils;
import org.springframework.context.annotation.Import;

import static org.assertj.core.api.Assertions.assertThat;

class PlatformIntegrationTestAnnotationTest {

    @PlatformIntegrationTest
    static class DefaultIntegrationTest {
    }

    @PlatformIntegrationTest(webEnvironment = SpringBootTest.WebEnvironment.NONE)
    static class NonWebIntegrationTest {
    }

    @Test
    void shouldDefaultToRandomPortAndImportHttpTestConfiguration() {
        var springBootTest = AnnotatedElementUtils.findMergedAnnotation(DefaultIntegrationTest.class, SpringBootTest.class);
        assertThat(springBootTest).isNotNull();
        assertThat(springBootTest.webEnvironment()).isEqualTo(SpringBootTest.WebEnvironment.RANDOM_PORT);
        var imported = AnnotatedElementUtils.findMergedAnnotation(DefaultIntegrationTest.class, Import.class);
        assertThat(imported).isNotNull();
        assertThat(imported.value()).contains(PlatformHttpTestConfiguration.class);
    }

    @Test
    void shouldAllowIntegrationTestsWithoutAnHttpServer() {
        var springBootTest = AnnotatedElementUtils.findMergedAnnotation(NonWebIntegrationTest.class, SpringBootTest.class);
        assertThat(springBootTest).isNotNull();
        assertThat(springBootTest.webEnvironment()).isEqualTo(SpringBootTest.WebEnvironment.NONE);
    }
}
