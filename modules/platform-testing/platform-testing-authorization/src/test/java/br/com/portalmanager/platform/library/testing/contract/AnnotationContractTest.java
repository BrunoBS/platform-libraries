package br.com.portalmanager.platform.library.testing.contract;

import br.com.portalmanager.platform.library.testing.authorization.AuthorizationMockResult;
import br.com.portalmanager.platform.library.testing.authorization.annotation.WithMockAuthorization;
import org.junit.jupiter.api.Test;
import org.junit.platform.commons.support.AnnotationSupport;

import static org.assertj.core.api.Assertions.assertThat;

class AnnotationContractTest {

    @WithMockAuthorization
    static class DefaultAuthorizationTest {
    }

    @Test
    void shouldAllowAuthorizationByDefault() {
        WithMockAuthorization annotation = AnnotationSupport
                .findAnnotation(DefaultAuthorizationTest.class, WithMockAuthorization.class)
                .orElseThrow();
        assertThat(annotation.defaultResult()).isEqualTo(AuthorizationMockResult.ALLOWED);
    }
}
