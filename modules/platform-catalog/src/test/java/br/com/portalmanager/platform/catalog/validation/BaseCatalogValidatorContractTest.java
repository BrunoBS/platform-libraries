package br.com.portalmanager.platform.catalog.validation;

import br.com.portalmanager.platform.catalog.dto.CatalogDTO;
import br.com.portalmanager.platform.catalog.model.BaseCatalogEntity;
import br.com.portalmanager.platform.catalog.repository.BaseCatalogRepository;
import br.com.portalmanager.platform.messaging.exception.ValidationException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import tools.jackson.databind.json.JsonMapper;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@DisplayName("Contrato - BaseCatalogValidator")
class BaseCatalogValidatorContractTest {

    private static final JsonMapper JSON = JsonMapper.builder().build();

    private TestRepository repository;
    private TestValidator validator;

    @BeforeEach
    void setUp() {
        repository = mock(TestRepository.class);
        validator = new TestValidator(repository);
    }

    @Test
    void deveRejeitarIdNoCreate() {
        assertValidation(
                () -> validator.validateForCreate(dto(1L, "ONE", "Label", "Descrição válida")),
                "id"
        );
    }

    @Test
    void deveValidarCamposObrigatorios() {
        assertValidation(
                () -> validator.validateForCreate(dto(null, null, null, null)),
                "name", "label", "description"
        );
    }

    @Test
    void deveValidarLimitesDaDescricao() {
        assertValidation(
                () -> validator.validateForCreate(dto(null, "ONE", "Label", "D")),
                "description"
        );

        assertValidation(
                () -> validator.validateForCreate(dto(null, "ONE", "Label", "A".repeat(251))),
                "description"
        );
    }

    @Test
    void deveRejeitarNomeDuplicado() {
        when(repository.existsByNameAndIdNot("ONE", 0L)).thenReturn(true);

        assertValidation(
                () -> validator.validateForCreate(dto(null, "ONE", "Label", "Descrição válida")),
                "name"
        );
    }

    @Test
    void devePermitirMesmoNomeDoRegistroNoUpdate() {
        TestEntity current = entity(10L, "ONE", 1);
        when(repository.findById(10L)).thenReturn(Optional.of(current));
        when(repository.existsByNameAndIdNot("ONE", 10L)).thenReturn(false);

        validator.validateForUpdate(dto(10L, "ONE", "Atualizado", "Descrição atualizada"));
    }

    @Test
    void deveRejeitarAlteracaoDoNomeNoUpdate() {
        TestEntity current = entity(10L, "ONE", 1);
        when(repository.findById(10L)).thenReturn(Optional.of(current));

        assertValidation(
                () -> validator.validateForUpdate(
                        dto(10L, "TWO", "Atualizado", "Descrição atualizada")
                ),
                "name"
        );
    }

    private CatalogDTO dto(Long id, String name, String label, String description) {
        return new CatalogDTO(id, name, label, description, 1, JSON.createObjectNode());
    }

    private void assertValidation(Runnable operation, String... fields) {
        assertThatThrownBy(operation::run)
                .isInstanceOfSatisfying(ValidationException.class, exception ->
                        assertThat(exception.getDetails())
                                .extracting(detail -> detail.field())
                                .contains(fields)
                );
    }

    private static TestEntity entity(Long id, String name, Integer sortOrder) {
        TestEntity entity = new TestEntity();
        entity.setTestId(id);
        entity.setName(name);
        entity.setLabel(name);
        entity.setDescription("Descrição válida");
        entity.setSortOrder(sortOrder);
        entity.setActive(true);
        entity.setSettings("{}");
        return entity;
    }

    private interface TestRepository extends BaseCatalogRepository<TestEntity> {
    }

    private static final class TestEntity extends BaseCatalogEntity {
        public TestEntity() {
        }

        void setTestId(Long id) {
            this.id = id;
        }
    }

    private static final class TestValidator extends BaseCatalogValidator<CatalogDTO> {

        private TestValidator(TestRepository repository) {
            super(repository);
        }

        @Override
        public String entityName() {
            return "TestCatalog";
        }
    }
}
