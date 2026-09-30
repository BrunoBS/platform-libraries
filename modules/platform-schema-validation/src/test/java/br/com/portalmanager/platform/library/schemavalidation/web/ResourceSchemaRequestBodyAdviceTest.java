package br.com.portalmanager.platform.library.schemavalidation.web;

import org.junit.jupiter.api.Test;
import org.springframework.web.bind.annotation.ControllerAdvice;

import static org.assertj.core.api.Assertions.assertThat;

class ResourceSchemaRequestBodyAdviceTest {

    @Test
    void mustBeRegisteredAsControllerAdvice() {
        assertThat(ResourceSchemaRequestBodyAdvice.class)
                .hasAnnotation(ControllerAdvice.class);
    }
}
