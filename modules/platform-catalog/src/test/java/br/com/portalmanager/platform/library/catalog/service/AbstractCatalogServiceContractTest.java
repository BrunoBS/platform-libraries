package br.com.portalmanager.platform.library.catalog.service;

import br.com.portalmanager.platform.library.catalog.dto.CatalogDTO;
import br.com.portalmanager.platform.library.catalog.model.CatalogEntity;
import br.com.portalmanager.platform.library.catalog.repository.CatalogRepository;
import br.com.portalmanager.platform.library.messaging.exception.ValidationException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import tools.jackson.databind.json.JsonMapper;

import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@DisplayName("Contrato - AbstractCatalogService")
class AbstractCatalogServiceContractTest {

    private static final JsonMapper JSON = JsonMapper.builder().build();

    private TestRepository repository;
    private TestService service;

    @BeforeEach
    void setUp() {
        repository = mock(TestRepository.class);
        service = new TestService(repository);

        when(repository.save(any(TestEntity.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));
    }

    @Test
    void deveAplicarProximoSortOrderNoCreate() {
        when(repository.findFirstByOrderBySortOrderDesc())
                .thenReturn(Optional.of(entity("EXISTING", 5)));

        CatalogDTO created = service.create(dto("ONE", null));

        assertThat(created.sortOrder()).isEqualTo(6);
    }

    @Test
    void devePreservarSortOrderExplicitoNoCreate() {
        CatalogDTO created = service.create(dto("ONE", 30));

        assertThat(created.sortOrder()).isEqualTo(30);
    }

    @Test
    void deveCalcularSortOrderAntesDeMapearUpdateNulo() {
        TestEntity current = entity("ONE", 5);
        when(repository.findByCodeAndActiveTrue("ONE")).thenReturn(Optional.of(current));
        when(repository.findFirstByCodeNotOrderBySortOrderDesc("ONE"))
                .thenReturn(Optional.of(entity("TWO", 10)));

        CatalogDTO updated = service.update("ONE", dto(null, null));

        assertThat(updated.sortOrder()).isEqualTo(11);
        assertThat(updated.code()).isEqualTo("ONE");
        verify(repository).findFirstByCodeNotOrderBySortOrderDesc("ONE");
    }

    @Test
    @SuppressWarnings("unchecked")
    void deveOrdenarListagemPorSortOrderECode() {
        when(repository.findAll(any(Specification.class), any(Sort.class)))
                .thenReturn(List.of());

        service.findAll(Map.of());

        verify(repository).findAll(
                any(Specification.class),
                eq(Sort.by(Sort.Order.asc("sortOrder"), Sort.Order.asc("code")))
        );
    }

    @Test
    void deveRejeitarFiltroNaoSuportado() {
        assertThatThrownBy(() -> service.findAll(Map.of("unknown", "value")))
                .isInstanceOf(ValidationException.class);
    }

    @Test
    void deveRejeitarBooleanoInvalido() {
        assertThatThrownBy(() -> service.findAll(Map.of("active", "invalid")))
                .isInstanceOf(ValidationException.class);
    }

    @Test
    void deveRejeitarUpdateComBodyNuloPeloContratoDeValidacao() {
        assertThatThrownBy(() -> service.update("ONE", null))
                .isInstanceOf(ValidationException.class);
    }

    @Test
    void deveAplicarSoftDelete() {
        TestEntity current = entity("ONE", 1);
        when(repository.findByCodeAndActiveTrue("ONE")).thenReturn(Optional.of(current));

        service.delete("ONE");

        assertThat(current.isActive()).isFalse();
        verify(repository).save(current);
    }

    @Test
    void deveRestaurarRegistroInativo() {
        TestEntity current = entity("ONE", 1);
        current.setActive(false);
        when(repository.findByCodeAndActiveFalse("ONE")).thenReturn(Optional.of(current));

        CatalogDTO restored = service.restore("ONE");

        assertThat(restored.code()).isEqualTo("ONE");
        assertThat(current.isActive()).isTrue();
    }

    private CatalogDTO dto(String code, Integer sortOrder) {
        return new CatalogDTO(
                code,
                code == null ? "Label" : code,
                "Descrição válida para catálogo",
                sortOrder,
                JSON.createObjectNode()
        );
    }

    private static TestEntity entity(String code, Integer sortOrder) {
        TestEntity entity = new TestEntity();
        entity.setCode(code);
        entity.setLabel(code);
        entity.setDescription("Descrição válida para catálogo");
        entity.setSortOrder(sortOrder);
        entity.setActive(true);
        entity.setSettings("{}");
        return entity;
    }

    private interface TestRepository extends CatalogRepository<TestEntity> {
    }

    public static final class TestEntity extends CatalogEntity {
        public TestEntity() {
        }
    }

    private static final class TestService
            extends IncludedCatalogService<TestEntity> {

        private TestService(TestRepository repository) {
            super(repository, JSON, TestEntity.class);
        }
    }
}
