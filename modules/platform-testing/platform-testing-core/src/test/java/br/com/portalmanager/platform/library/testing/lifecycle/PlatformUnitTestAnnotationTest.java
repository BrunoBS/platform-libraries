package br.com.portalmanager.platform.library.testing.lifecycle;

import br.com.portalmanager.platform.library.testing.lifecycle.annotation.PlatformUnitTest;
import org.junit.jupiter.api.Test;
import org.junit.platform.commons.support.AnnotationSupport;

import static org.assertj.core.api.Assertions.assertThat;

class PlatformUnitTestAnnotationTest {

    @PlatformUnitTest
    static class ExampleUnitTest {
    }

    @Test
    void shouldExposePlatformUnitTestAnnotation() {
        assertThat(AnnotationSupport.findAnnotation(ExampleUnitTest.class, PlatformUnitTest.class)).isPresent();
    }
}
