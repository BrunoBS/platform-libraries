package br.com.portalmanager.platform.library.catalog.internal.mapping;

import br.com.portalmanager.platform.library.catalog.api.contract.CatalogDTO;
import br.com.portalmanager.platform.library.catalog.api.exception.CatalogTechnicalException;
import br.com.portalmanager.platform.library.catalog.internal.message.CatalogMessageKeys;
import br.com.portalmanager.platform.library.catalog.api.contract.model.CatalogEntity;
import org.junit.jupiter.api.Test;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.json.JsonMapper;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class CatalogMapperTest {

    private static final JsonMapper JSON = JsonMapper.builder().build();

    @Test
    void shouldMapCodeAsIdentityAndKeepItImmutableOnUpdate() {
        CatalogMapper<TestCatalog> mapper =
                new CatalogMapper<>(TestCatalog.class, new ObjectMapper());

        TestCatalog entity = mapper.toEntity(new CatalogDTO(
                "ONE",
                "One",
                "Descrição válida",
                1,
                JSON.createObjectNode()
        ));

        mapper.updateEntity(entity, new CatalogDTO(
                "TWO",
                "Updated",
                "Descrição atualizada",
                2,
                JSON.createObjectNode()
        ));

        assertThat(entity.getCode()).isEqualTo("ONE");
        assertThat(entity.getLabel()).isEqualTo("Updated");
    }

    @Test
    void shouldFailFastWhenStoredSettingsJsonIsInvalid() {
        TestCatalog entity = new TestCatalog();
        entity.setCode("ONE");
        entity.setSettings("{invalid-json");

        CatalogMapper<TestCatalog> mapper =
                new CatalogMapper<>(TestCatalog.class, new ObjectMapper());

        assertThatThrownBy(() -> mapper.toDTO(entity))
                .isInstanceOfSatisfying(CatalogTechnicalException.class, exception -> {
                    assertThat(exception.getMessageKey())
                            .isEqualTo(CatalogMessageKeys.SETTINGS_INVALID_STORED_JSON);
                    assertThat(exception.getCause()).isNotNull();
                });
    }

    @Test
    void shouldFailWithStandardTechnicalExceptionWhenEntityCannotBeInstantiated() {
        CatalogMapper<InvalidCatalog> mapper =
                new CatalogMapper<>(InvalidCatalog.class, new ObjectMapper());

        assertThatThrownBy(() -> mapper.toEntity(new CatalogDTO(
                "ONE",
                "One",
                "Descrição válida",
                1,
                JSON.createObjectNode()
        )))
                .isInstanceOfSatisfying(CatalogTechnicalException.class, exception -> {
                    assertThat(exception.getMessageKey())
                            .isEqualTo(CatalogMessageKeys.ENTITY_INSTANTIATION_FAILED);
                    assertThat(exception.getCause()).isNotNull();
                });
    }

    public static class TestCatalog extends CatalogEntity {
        public TestCatalog() {
        }
    }

    public static class InvalidCatalog extends CatalogEntity {
        private InvalidCatalog(String ignored) {
        }
    }
}
