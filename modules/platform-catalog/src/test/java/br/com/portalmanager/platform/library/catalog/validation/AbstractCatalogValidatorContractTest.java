package br.com.portalmanager.platform.library.catalog.validation;

import br.com.portalmanager.platform.library.catalog.dto.CatalogDTO;
import br.com.portalmanager.platform.library.catalog.model.CatalogEntity;
import br.com.portalmanager.platform.library.catalog.repository.CatalogRepository;
import br.com.portalmanager.platform.library.messaging.exception.ValidationException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import tools.jackson.databind.json.JsonMapper;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@DisplayName("Contrato - AbstractCatalogValidator")
class AbstractCatalogValidatorContractTest {

    private static final JsonMapper JSON = JsonMapper.builder().build();

    private TestRepository repository;
    private TestValidator validator;

    @BeforeEach
    void setUp() {
        repository = mock(TestRepository.class);
        validator = new TestValidator(repository);
    }

    @Test
    void deveValidarCamposObrigatorios() {
        assertValidation(
                () -> validator.validateForCreate(dto(null, null, null)),
                "code", "label", "description"
        );
    }

    @Test
    void deveValidarFormatoDoCode() {
        assertValidation(
                () -> validator.validateForCreate(dto("invalid-code", "Label", "Descrição válida")),
                "code"
        );

        assertValidation(
                () -> validator.validateForCreate(dto("1INVALID", "Label", "Descrição válida")),
                "code"
        );

        assertValidation(
                () -> validator.validateForCreate(dto("A".repeat(51), "Label", "Descrição válida")),
                "code"
        );
    }

    @Test
    void deveValidarLimitesDaDescricao() {
        assertValidation(
                () -> validator.validateForCreate(dto("ONE", "Label", "D")),
                "description"
        );

        assertValidation(
                () -> validator.validateForCreate(dto("ONE", "Label", "A".repeat(251))),
                "description"
        );
    }

    @Test
    void deveRejeitarCodeDuplicado() {
        when(repository.existsById("ONE")).thenReturn(true);

        assertValidation(
                () -> validator.validateForCreate(dto("ONE", "Label", "Descrição válida")),
                "code"
        );
    }

    @Test
    void devePermitirUpdateDoMesmoCode() {
        validator.validateForUpdate(dto("ONE", "Atualizado", "Descrição atualizada"));
    }

    private CatalogDTO dto(String code, String label, String description) {
        return new CatalogDTO(code, label, description, 1, JSON.createObjectNode());
    }

    private void assertValidation(Runnable operation, String... fields) {
        assertThatThrownBy(operation::run)
                .isInstanceOfSatisfying(ValidationException.class, exception ->
                        assertThat(exception.getDetails())
                                .extracting(detail -> detail.field())
                                .contains(fields)
                );
    }

    private interface TestRepository extends CatalogRepository<TestEntity> {
    }

    private static final class TestEntity extends CatalogEntity {
        public TestEntity() {
        }
    }

    private static final class TestValidator extends AbstractCatalogValidator {

        private TestValidator(TestRepository repository) {
            super(repository);
        }

        @Override
        public String entityName() {
            return "TestCatalog";
        }
    }
}
