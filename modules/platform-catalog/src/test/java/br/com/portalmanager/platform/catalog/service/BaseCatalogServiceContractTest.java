package br.com.portalmanager.platform.catalog.service;

import br.com.portalmanager.platform.catalog.dto.CatalogDTO;
import br.com.portalmanager.platform.catalog.mapper.BaseCatalogMapper;
import br.com.portalmanager.platform.catalog.model.BaseCatalogEntity;
import br.com.portalmanager.platform.catalog.repository.BaseCatalogRepository;
import br.com.portalmanager.platform.catalog.validation.BaseCatalogValidator;
import br.com.portalmanager.platform.messaging.exception.ValidationException;
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

@DisplayName("Contrato - BaseCatalogService")
class BaseCatalogServiceContractTest {

    // BaseCatalogService is intentionally package-private. External consumers must
    // extend EnumCatalogService or DynamicCatalogService instead.

    private static final JsonMapper JSON = JsonMapper.builder().build();

    private TestRepository repository;
    private BaseCatalogValidator<CatalogDTO> validator;
    private TestService service;

    @BeforeEach
    @SuppressWarnings("unchecked")
    void setUp() {
        repository = mock(TestRepository.class);
        validator = mock(BaseCatalogValidator.class);
        service = new TestService(repository, new TestMapper(), validator);

        when(repository.save(any(TestEntity.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));
    }

    @Test
    void deveAplicarProximoSortOrderNoCreate() {
        when(repository.findFirstByOrderBySortOrderDesc())
                .thenReturn(Optional.of(entity(1L, "EXISTING", 5)));

        CatalogDTO created = service.create(dto(null, "ONE", null));

        assertThat(created.sortOrder()).isEqualTo(6);
    }

    @Test
    void devePreservarSortOrderExplicitoNoCreate() {
        CatalogDTO created = service.create(dto(null, "ONE", 30));

        assertThat(created.sortOrder()).isEqualTo(30);
    }

    @Test
    void deveCalcularSortOrderAntesDeMapearUpdateNulo() {
        TestEntity current = entity(10L, "ONE", 5);
        when(repository.findByIdAndActiveTrue(10L)).thenReturn(Optional.of(current));
        when(repository.findFirstByIdNotOrderBySortOrderDesc(10L))
                .thenReturn(Optional.of(entity(20L, "TWO", 10)));

        CatalogDTO updated = service.update(10L, dto(null, "ONE", null));

        assertThat(updated.sortOrder()).isEqualTo(11);
        verify(repository).findFirstByIdNotOrderBySortOrderDesc(10L);
    }

    @Test
    @SuppressWarnings("unchecked")
    void deveOrdenarListagemPorSortOrderEId() {
        when(repository.findAll(any(Specification.class), any(Sort.class)))
                .thenReturn(List.of());

        service.findAll(Map.of());

        verify(repository).findAll(
                any(Specification.class),
                eq(Sort.by(Sort.Order.asc("sortOrder"), Sort.Order.asc("id")))
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
    void deveAplicarSoftDelete() {
        TestEntity current = entity(10L, "ONE", 1);
        when(repository.findByIdAndActiveTrue(10L)).thenReturn(Optional.of(current));

        service.delete(10L);

        assertThat(current.isActive()).isFalse();
        verify(repository).save(current);
    }

    @Test
    void deveRestaurarRegistroInativo() {
        TestEntity current = entity(10L, "ONE", 1);
        current.setActive(false);
        when(repository.findByIdAndActiveFalse(10L)).thenReturn(Optional.of(current));

        CatalogDTO restored = service.restore(10L);

        assertThat(restored.id()).isEqualTo(10L);
        assertThat(current.isActive()).isTrue();
    }

    private CatalogDTO dto(Long id, String name, Integer sortOrder) {
        return new CatalogDTO(
                id,
                name,
                name,
                "Descrição válida para catálogo",
                sortOrder,
                JSON.createObjectNode()
        );
    }

    private static TestEntity entity(Long id, String name, Integer sortOrder) {
        TestEntity entity = new TestEntity();
        entity.setTestId(id);
        entity.setName(name);
        entity.setLabel(name);
        entity.setDescription("Descrição válida para catálogo");
        entity.setSortOrder(sortOrder);
        entity.setActive(true);
        entity.setSettings("{}");
        return entity;
    }

    private interface TestRepository extends BaseCatalogRepository<TestEntity> {
    }

    public static final class TestEntity extends BaseCatalogEntity {
        public TestEntity() {
        }

        void setTestId(Long id) {
            this.id = id;
        }
    }

    private static final class TestMapper extends BaseCatalogMapper<CatalogDTO, TestEntity> {

        private TestMapper() {
            super(TestEntity.class);
        }

        @Override
        public TestEntity toEntity(CatalogDTO dto) {
            if (dto == null) {
                return null;
            }
            TestEntity entity = new TestEntity();
            mapCommonFields(entity, dto);
            entity.setActive(true);
            return entity;
        }

        @Override
        public CatalogDTO toDTO(TestEntity entity) {
            if (entity == null) {
                return null;
            }
            return new CatalogDTO(
                    entity.getId(),
                    entity.getName(),
                    entity.getLabel(),
                    entity.getDescription(),
                    entity.getSortOrder(),
                    JSON.createObjectNode()
            );
        }
    }

    private static final class TestService
            extends BaseCatalogService<TestEntity, CatalogDTO> {

        private TestService(
                TestRepository repository,
                TestMapper mapper,
                BaseCatalogValidator<CatalogDTO> validator
        ) {
            super(repository, mapper, validator);
        }
    }
}
