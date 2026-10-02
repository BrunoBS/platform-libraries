package br.com.portalmanager.platform.library.schemavalidation.config;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class PlatformSchemaValidationPropertiesTest {

    @Test
    void shouldExposeGoldenDefaults() {
        PlatformSchemaValidationProperties properties = new PlatformSchemaValidationProperties();

        assertThat(properties.resolveFallbackCode()).isEqualTo("DEFAULT");
        assertThat(properties.resolveViewName())
                .isEqualTo("vw_platform_resource_schemas");
    }

    @Test
    void shouldFallbackWhenTextConfigurationIsBlank() {
        PlatformSchemaValidationProperties properties = new PlatformSchemaValidationProperties();
        properties.setFallbackCode(" ");
        properties.setViewName(null);

        assertThat(properties.resolveFallbackCode()).isEqualTo("DEFAULT");
        assertThat(properties.resolveViewName())
                .isEqualTo("vw_platform_resource_schemas");
    }

    @Test
    void shouldNormalizeConfiguredTextValues() {
        PlatformSchemaValidationProperties properties = new PlatformSchemaValidationProperties();
        properties.setFallbackCode("  PLATFORM_DEFAULT  ");
        properties.setViewName("  custom_schema_view  ");

        assertThat(properties.resolveFallbackCode()).isEqualTo("PLATFORM_DEFAULT");
        assertThat(properties.resolveViewName()).isEqualTo("custom_schema_view");
    }
}
