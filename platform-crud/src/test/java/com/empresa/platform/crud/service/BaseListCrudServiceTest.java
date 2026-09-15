package com.empresa.platform.crud.service;

import com.empresa.platform.crud.dto.BaseCrudDTO;
import com.empresa.platform.crud.mapper.BaseCrudMapper;
import com.empresa.platform.crud.repository.BaseCrudRepository;
import com.empresa.platform.crud.validation.BaseCrudValidator;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.*;

class BaseListCrudServiceTest {

    private BaseCrudRepository<TestEntity, Long> repository;
    private BaseCrudMapper<TestEntity, TestDTO> mapper;
    private BaseCrudValidator<TestDTO> validator;
    private TestService service;

    @BeforeEach
    void setUp() {
        repository = mock(BaseCrudRepository.class);
        mapper = mock(BaseCrudMapper.class);
        validator = mock(BaseCrudValidator.class);
        service = new TestService(repository, mapper, validator);
    }

    @Test
    void shouldUseFindAllEntitiesHookAndMapResults() {
        TestEntity entity = new TestEntity(1L, "one");
        TestDTO dto = new TestDTO(1L, "one");

        when(mapper.toDTO(entity)).thenReturn(dto);
        service.entities = List.of(entity);

        assertThat(service.findAll(Map.of("active", "true")))
                .containsExactly(dto);

        assertThat(service.filters)
                .containsEntry("active", "true");
    }

    @Test
    void shouldRejectUnsupportedFilterBeforeQueryExecution() {
        assertThatThrownBy(() ->
                service.findAll(Map.of("unknown", "value")))
                .isInstanceOf(TestInvalidFilterException.class)
                .hasMessage("unsupported filter unknown=value");

        assertThat(service.filters).isEmpty();
    }

    @Test
    void shouldParseBooleanFilterStrictly() {
        assertThat(
                service.readBoolean(Map.of(), "active", true)
        ).isTrue();

        assertThat(
                service.readBoolean(
                        Map.of("active", "false"),
                        "active",
                        true
                )
        ).isFalse();

        assertThat(
                service.readBoolean(
                        Map.of("active", "TRUE"),
                        "active",
                        false
                )
        ).isTrue();

        assertThatThrownBy(() ->
                service.readBoolean(
                        Map.of("active", "invalid"),
                        "active",
                        true
                ))
                .isInstanceOf(TestInvalidFilterException.class)
                .hasMessage("invalid filter active=invalid");
    }

    record TestDTO(Long id, String name)
            implements BaseCrudDTO<Long> {
    }

    record TestEntity(Long id, String name) {
    }

    static class TestService extends BaseListCrudService<
            TestEntity,
            TestDTO,
            Long,
            BaseCrudRepository<TestEntity, Long>> {

        private List<TestEntity> entities = List.of();
        private Map<String, String> filters = Map.of();

        TestService(
                BaseCrudRepository<TestEntity, Long> repository,
                BaseCrudMapper<TestEntity, TestDTO> mapper,
                BaseCrudValidator<TestDTO> validator) {
            super(repository, mapper, validator);
        }

        @Override
        protected Set<String> allowedFilters() {
            return Set.of("active");
        }

        @Override
        protected List<TestEntity> findAllEntities(
                Map<String, String> filters) {
            this.filters = filters;
            return entities;
        }

        boolean readBoolean(
                Map<String, String> filters,
                String name,
                boolean defaultValue) {
            return booleanFilter(
                    filters,
                    name,
                    defaultValue
            );
        }

        @Override
        protected RuntimeException unsupportedFilterException(
                String name,
                String value) {
            return new TestInvalidFilterException(
                    "unsupported filter " + name + "=" + value
            );
        }

        @Override
        protected RuntimeException invalidFilterException(
                String name,
                String value) {
            return new TestInvalidFilterException(
                    "invalid filter " + name + "=" + value
            );
        }

        @Override
        protected RuntimeException notFoundException(Long id) {
            return new RuntimeException();
        }
    }

    static class TestInvalidFilterException extends RuntimeException {
        TestInvalidFilterException(String message) {
            super(message);
        }
    }
}
