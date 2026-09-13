package com.empresa.platform.crud.service;

import com.empresa.platform.crud.dto.BaseCrudDTO;
import com.empresa.platform.crud.mapper.BaseCrudMapper;
import com.empresa.platform.crud.repository.BaseCrudRepository;
import com.empresa.platform.crud.validation.BaseCrudValidator;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class BaseCrudServiceTest {

    private BaseCrudRepository<TestEntity, Long> repository;
    private BaseCrudMapper<TestEntity, TestDTO> mapper;
    private BaseCrudValidator<TestDTO, Long> validator;
    private TestService service;

    @BeforeEach
    void setUp() {
        repository = mock(BaseCrudRepository.class);
        mapper = mock(BaseCrudMapper.class);
        validator = mock(BaseCrudValidator.class);
        service = new TestService(repository, mapper, validator);
    }

    @Test
    void shouldNormalizeIdOnCreate() {
        TestDTO input = new TestDTO(99L, "name");
        TestDTO normalized = new TestDTO(null, "name");
        TestEntity entity = new TestEntity(null, "name");
        TestEntity saved = new TestEntity(1L, "name");
        TestDTO response = new TestDTO(1L, "name");

        when(mapper.toEntity(normalized)).thenReturn(entity);
        when(repository.save(entity)).thenReturn(saved);
        when(mapper.toDTO(saved)).thenReturn(response);

        assertThat(service.create(input)).isEqualTo(response);
        verify(validator).validateForCreate(normalized);
    }

    @Test
    void shouldNormalizePathIdOnUpdate() {
        TestDTO input = new TestDTO(99L, "updated");
        TestDTO normalized = new TestDTO(10L, "updated");
        TestEntity entity = new TestEntity(10L, "old");

        when(repository.findById(10L)).thenReturn(Optional.of(entity));
        when(repository.save(entity)).thenReturn(entity);
        when(mapper.toDTO(entity)).thenReturn(normalized);

        assertThat(service.update(10L, input)).isEqualTo(normalized);
        verify(validator).validateForUpdate(10L, normalized);
        verify(mapper).updateEntity(entity, normalized);
    }

    @Test
    void shouldDeleteExistingEntity() {
        TestEntity entity = new TestEntity(10L, "name");
        when(repository.findById(10L)).thenReturn(Optional.of(entity));

        service.delete(10L);

        verify(validator).validateForDelete(10L);
        verify(repository).delete(entity);
    }

    @Test
    void shouldDelegateMissingResourceExceptionToConsumer() {
        when(repository.findById(10L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.findById(10L))
                .isInstanceOf(TestNotFoundException.class)
                .hasMessage("test resource 10 not found");
    }

    record TestDTO(Long id, String name) implements BaseCrudDTO<Long, TestDTO> {
        @Override
        public TestDTO withId(Long id) {
            return new TestDTO(id, name);
        }
    }

    static class TestEntity {
        private final Long id;
        private final String name;

        TestEntity(Long id, String name) {
            this.id = id;
            this.name = name;
        }
    }

    static class TestService extends BaseCrudService<TestEntity, TestDTO, Long> {
        TestService(
                BaseCrudRepository<TestEntity, Long> repository,
                BaseCrudMapper<TestEntity, TestDTO> mapper,
                BaseCrudValidator<TestDTO, Long> validator) {
            super(repository, mapper, validator);
        }

        @Override
        protected RuntimeException notFoundException(Long id) {
            return new TestNotFoundException("test resource " + id + " not found");
        }
    }

    static class TestNotFoundException extends RuntimeException {
        TestNotFoundException(String message) {
            super(message);
        }
    }
}
