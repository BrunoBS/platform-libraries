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
import static org.mockito.Mockito.*;

class BaseCrudServiceTest {

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
    void shouldUseContextualDtoOnUpdate() {
        TestDTO dto = new TestDTO(10L, "updated");
        TestEntity entity = new TestEntity(10L, "old");

        when(repository.findById(10L)).thenReturn(Optional.of(entity));
        when(repository.save(entity)).thenReturn(entity);
        when(mapper.toDTO(entity)).thenReturn(dto);

        assertThat(service.update(dto)).isEqualTo(dto);
        verify(validator).validateForUpdate(dto);
        verify(mapper).updateEntity(entity, dto);
        assertThat(service.entityContext).isEqualTo(dto);
    }

    @Test
    void shouldDeleteExistingEntity() {
        TestEntity entity = new TestEntity(10L, "name");
        when(repository.findById(10L)).thenReturn(Optional.of(entity));

        TestDTO reference = new TestDTO(10L, null);
        service.delete(reference);

        verify(validator).validateForDelete(reference);
        verify(repository).delete(entity);
    }

    @Test
    void shouldDelegateMissingResourceExceptionToConsumer() {
        when(repository.findById(10L)).thenReturn(Optional.empty());

        TestDTO reference = new TestDTO(10L, null);

        assertThatThrownBy(() -> service.findById(reference))
                .isInstanceOf(TestNotFoundException.class)
                .hasMessage("test resource 10 not found");
    }

    record TestDTO(Long id, String name)
            implements BaseCrudDTO<Long, TestDTO> {

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

    static class TestService extends BaseCrudService<
            TestEntity,
            TestDTO,
            Long,
            BaseCrudRepository<TestEntity, Long>> {

        private TestDTO entityContext;

        TestService(
                BaseCrudRepository<TestEntity, Long> repository,
                BaseCrudMapper<TestEntity, TestDTO> mapper,
                BaseCrudValidator<TestDTO> validator) {
            super(repository, mapper, validator);
        }

        @Override
        protected TestEntity getEntity(TestDTO dto) {
            this.entityContext = dto;
            return super.getEntity(dto);
        }

        @Override
        protected RuntimeException notFoundException(Long id) {
            return new TestNotFoundException(
                    "test resource " + id + " not found"
            );
        }
    }

    static class TestNotFoundException extends RuntimeException {
        TestNotFoundException(String message) {
            super(message);
        }
    }
}
