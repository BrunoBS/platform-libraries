package br.com.portalmanager.platform.library.schemavalidation.resolver;

import br.com.portalmanager.platform.library.schemavalidation.config.PlatformSchemaValidationProperties;
import br.com.portalmanager.platform.library.schemavalidation.cache.ResourceSchemaCache;
import br.com.portalmanager.platform.library.schemavalidation.model.ResourceSchema;
import br.com.portalmanager.platform.library.schemavalidation.repository.ResourceSchemaRepository;
import br.com.portalmanager.platform.library.messaging.exception.PlatformConfigurationException;
import org.junit.jupiter.api.Test;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class DefaultResourceSchemaResolverTest {

    @Test
    void resolvesSpecificSchemaBeforeDefault() {
        ResourceSchema specific = new ResourceSchema("APPLICATION", "application", 2, "{}");
        ResourceSchemaRepository repository = (type, code) ->
                "application".equals(code) ? Optional.of(specific) : Optional.empty();

        var resolver = new DefaultResourceSchemaResolver(repository, new PlatformSchemaValidationProperties());

        assertThat(resolver.resolve("APPLICATION", "application")).isEqualTo(specific);
    }

    @Test
    void fallsBackToDefaultOfSameResourceType() {
        ResourceSchema fallback = new ResourceSchema("MENU", "DEFAULT", 1, "{}");
        ResourceSchemaRepository repository = (type, code) ->
                "DEFAULT".equals(code) ? Optional.of(fallback) : Optional.empty();

        var resolver = new DefaultResourceSchemaResolver(repository, new PlatformSchemaValidationProperties());

        assertThat(resolver.resolve("MENU", "menu")).isEqualTo(fallback);
    }

    @Test
    void failsWhenSpecificAndDefaultAreMissing() {
        ResourceSchemaRepository repository = (type, code) -> Optional.empty();
        var resolver = new DefaultResourceSchemaResolver(repository, new PlatformSchemaValidationProperties());

        assertThatThrownBy(() -> resolver.resolve("MENU", "menu"))
                .isInstanceOf(PlatformConfigurationException.class)
                .hasMessageContaining("MENU/menu");
    }

    @Test
    void usesSharedCacheBeforeDatasource() {
        ResourceSchema cached = new ResourceSchema("APPLICATION", "application", 3, "{}");
        ResourceSchemaCache cache = new ResourceSchemaCache() {
            public Optional<ResourceSchema> get(String type, String code) { return Optional.of(cached); }
            public void put(ResourceSchema schema) { }
        };
        ResourceSchemaRepository repository = (type, code) -> {
            throw new AssertionError("Datasource should not be called on cache hit");
        };

        var resolver = new DefaultResourceSchemaResolver(
                repository, new PlatformSchemaValidationProperties(), cache
        );

        assertThat(resolver.resolve("APPLICATION", "application")).isEqualTo(cached);
    }

    @Test
    void continuesToDatasourceWhenSharedCacheFails() {
        ResourceSchema expected = new ResourceSchema("MENU", "menu", 2, "{}");
        ResourceSchemaCache cache = new ResourceSchemaCache() {
            public Optional<ResourceSchema> get(String type, String code) { throw new IllegalStateException("redis down"); }
            public void put(ResourceSchema schema) { throw new IllegalStateException("redis down"); }
        };
        ResourceSchemaRepository repository = (type, code) ->
                "menu".equals(code) ? Optional.of(expected) : Optional.empty();

        var resolver = new DefaultResourceSchemaResolver(
                repository, new PlatformSchemaValidationProperties(), cache
        );

        assertThat(resolver.resolve("MENU", "menu")).isEqualTo(expected);
    }
}

