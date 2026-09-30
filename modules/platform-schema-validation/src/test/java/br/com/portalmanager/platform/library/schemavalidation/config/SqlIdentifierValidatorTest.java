package br.com.portalmanager.platform.library.schemavalidation.config;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class SqlIdentifierValidatorTest {

    @Test
    void acceptsStrictViewName() {
        assertThat(SqlIdentifierValidator.validate("vw_platform_resource_schemas"))
                .isEqualTo("vw_platform_resource_schemas");
    }

    @Test
    void rejectsUnsafeViewName() {
        assertThatThrownBy(() -> SqlIdentifierValidator.validate("schema.view;drop"))
                .isInstanceOf(IllegalStateException.class);
    }
}
