package br.com.portalmanager.platform.library.testing.http;

import br.com.portalmanager.platform.library.testing.http.annotation.WithHttpMock;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.parallel.ResourceLock;
import org.springframework.context.annotation.Import;
import org.springframework.core.annotation.AnnotatedElementUtils;

import static org.assertj.core.api.Assertions.assertThat;

class WithHttpMockAnnotationTest {

    @Test
    void importsOneLifecycleAndSerializesSharedServerAccess() {
        assertThat(AnnotatedElementUtils.findMergedAnnotation(WithHttpMock.class, Import.class).value())
                .contains(HttpMockTestConfiguration.class);
        assertThat(AnnotatedElementUtils.findMergedAnnotation(WithHttpMock.class, ExtendWith.class).value())
                .contains(HttpMockExtension.class);
        assertThat(AnnotatedElementUtils.findMergedAnnotation(WithHttpMock.class, ResourceLock.class).value())
                .isEqualTo("platform-http-mock");
    }
}
