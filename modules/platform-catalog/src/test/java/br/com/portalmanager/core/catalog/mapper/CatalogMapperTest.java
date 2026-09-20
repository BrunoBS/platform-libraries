package br.com.portalmanager.core.catalog.mapper;

import br.com.portalmanager.core.catalog.model.BaseCatalogEntity;
import org.junit.jupiter.api.Test;
import tools.jackson.databind.ObjectMapper;

import static org.assertj.core.api.Assertions.assertThatThrownBy;

class CatalogMapperTest {

    @Test
    void shouldFailFastWhenStoredSettingsJsonIsInvalid() {
        TestCatalog entity = new TestCatalog();
        entity.setSettings("{invalid-json");

        CatalogMapper<TestCatalog> mapper =
                new CatalogMapper<>(TestCatalog.class, new ObjectMapper());

        assertThatThrownBy(() -> mapper.toDTO(entity))
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("Invalid catalog settings JSON stored in database");
    }

    public static class TestCatalog extends BaseCatalogEntity {
        public TestCatalog() {
        }
    }
}
