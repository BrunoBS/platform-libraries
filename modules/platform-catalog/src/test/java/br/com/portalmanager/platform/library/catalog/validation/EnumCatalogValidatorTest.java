package br.com.portalmanager.platform.library.catalog.validation;

import br.com.portalmanager.platform.library.catalog.dto.CatalogDTO;
import br.com.portalmanager.platform.library.catalog.model.CatalogEntity;
import br.com.portalmanager.platform.library.catalog.model.CatalogEnum;
import br.com.portalmanager.platform.library.catalog.repository.CatalogRepository;
import br.com.portalmanager.platform.library.messaging.exception.ValidationException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import tools.jackson.databind.json.JsonMapper;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;

class EnumCatalogValidatorTest {

    private static final JsonMapper JSON = JsonMapper.builder().build();

    private TestValidator validator;

    @BeforeEach
    void setUp() {
        validator = new TestValidator(mock(TestRepository.class));
    }

    @Test
    void shouldAcceptCodeDeclaredByEnum() {
        validator.validateForCreate(dto("ONE"));
    }

    @Test
    void shouldRejectCodeNotDeclaredByEnum() {
        assertThatThrownBy(() -> validator.validateForCreate(dto("UNKNOWN")))
                .isInstanceOfSatisfying(ValidationException.class, exception ->
                        assertThat(exception.getDetails())
                                .anySatisfy(detail -> {
                                    assertThat(detail.field()).isEqualTo("code");
                                    assertThat(detail.parameters()).containsValue("ONE, TWO");
                                })
                );
    }

    private CatalogDTO dto(String code) {
        return new CatalogDTO(
                code,
                "Label",
                "Descrição válida para catálogo",
                1,
                JSON.createObjectNode()
        );
    }

    private enum TestCatalog implements CatalogEnum<TestCatalog> {
        ONE,
        TWO
    }

    private interface TestRepository extends CatalogRepository<TestEntity> {
    }

    private static final class TestEntity extends CatalogEntity {
        public TestEntity() {
        }
    }

    private static final class TestValidator extends EnumCatalogValidator<TestCatalog> {

        private TestValidator(TestRepository repository) {
            super(repository, TestCatalog.class);
        }

        @Override
        public String entityName() {
            return "TestCatalog";
        }
    }
}
