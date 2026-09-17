package com.empresa.platform.crud.service;

import com.empresa.platform.crud.dto.BaseCrudDTO;
import com.empresa.platform.crud.dto.VersionedCrudDTO;
import com.empresa.platform.crud.mapper.BaseCrudMapper;
import com.empresa.platform.crud.repository.BaseCrudRepository;
import com.empresa.platform.crud.validation.BaseCrudValidator;
import com.empresa.platform.crud.version.OptimisticLockable;
import com.empresa.platform.messaging.exception.ResourceVersionConflictException;
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
    void shouldUseReceivedDtoOnCreate() {
        TestDTO input = new TestDTO(null, "name");
        TestEntity entity = new TestEntity(null, "name");
        TestEntity saved = new TestEntity(1L, "name");
        TestDTO response = new TestDTO(1L, "name");

        when(mapper.toEntity(input)).thenReturn(entity);
        when(repository.save(entity)).thenReturn(saved);
        when(mapper.toDTO(saved)).thenReturn(response);

        assertThat(service.create(input)).isEqualTo(response);
        verify(validator).validateForCreate(input);
        verify(mapper).toEntity(input);
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
    void shouldAcceptMatchingVersionOnUpdate() {
        BaseCrudRepository<VersionedTestEntity, Long> versionedRepository = mock(BaseCrudRepository.class);
        BaseCrudMapper<VersionedTestEntity, VersionedTestDTO> versionedMapper = mock(BaseCrudMapper.class);
        BaseCrudValidator<VersionedTestDTO> versionedValidator = mock(BaseCrudValidator.class);
        VersionedTestService versionedService = new VersionedTestService(
                versionedRepository,
                versionedMapper,
                versionedValidator
        );

        VersionedTestDTO dto = new VersionedTestDTO(10L, 3L, "updated");
        VersionedTestEntity entity = new VersionedTestEntity(10L, 3L, "old");

        when(versionedRepository.findById(10L)).thenReturn(Optional.of(entity));
        when(versionedRepository.save(entity)).thenReturn(entity);
        when(versionedMapper.toDTO(entity)).thenReturn(dto);

        assertThat(versionedService.update(dto)).isEqualTo(dto);
        verify(versionedMapper).updateEntity(entity, dto);
        verify(versionedRepository).save(entity);
    }

    @Test
    void shouldRejectStaleVersionBeforeMappingEntity() {
        BaseCrudRepository<VersionedTestEntity, Long> versionedRepository = mock(BaseCrudRepository.class);
        BaseCrudMapper<VersionedTestEntity, VersionedTestDTO> versionedMapper = mock(BaseCrudMapper.class);
        BaseCrudValidator<VersionedTestDTO> versionedValidator = mock(BaseCrudValidator.class);
        VersionedTestService versionedService = new VersionedTestService(
                versionedRepository,
                versionedMapper,
                versionedValidator
        );

        VersionedTestDTO dto = new VersionedTestDTO(10L, 2L, "updated");
        VersionedTestEntity entity = new VersionedTestEntity(10L, 3L, "old");

        when(versionedRepository.findById(10L)).thenReturn(Optional.of(entity));

        assertThatThrownBy(() -> versionedService.update(dto))
                .isInstanceOf(ResourceVersionConflictException.class);

        verify(versionedMapper, never()).updateEntity(any(), any());
        verify(versionedRepository, never()).save(any());
    }

    @Test
    void shouldRejectMissingVersionForVersionedResource() {
        BaseCrudRepository<VersionedTestEntity, Long> versionedRepository = mock(BaseCrudRepository.class);
        BaseCrudMapper<VersionedTestEntity, VersionedTestDTO> versionedMapper = mock(BaseCrudMapper.class);
        BaseCrudValidator<VersionedTestDTO> versionedValidator = mock(BaseCrudValidator.class);
        VersionedTestService versionedService = new VersionedTestService(
                versionedRepository,
                versionedMapper,
                versionedValidator
        );

        VersionedTestDTO dto = new VersionedTestDTO(10L, null, "updated");
        VersionedTestEntity entity = new VersionedTestEntity(10L, 3L, "old");

        when(versionedRepository.findById(10L)).thenReturn(Optional.of(entity));

        assertThatThrownBy(() -> versionedService.update(dto))
                .isInstanceOf(ResourceVersionConflictException.class);

        verify(versionedMapper, never()).updateEntity(any(), any());
        verify(versionedRepository, never()).save(any());
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
            implements BaseCrudDTO<Long> {
    }

    record VersionedTestDTO(Long id, Long version, String name)
            implements VersionedCrudDTO<Long> {
    }

    static class TestEntity {
        private final Long id;
        private final String name;

        TestEntity(Long id, String name) {
            this.id = id;
            this.name = name;
        }
    }

    static class VersionedTestEntity implements OptimisticLockable {
        private final Long id;
        private final Long version;
        private final String name;

        VersionedTestEntity(Long id, Long version, String name) {
            this.id = id;
            this.version = version;
            this.name = name;
        }

        @Override
        public Long getVersion() {
            return version;
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

    static class VersionedTestService extends BaseCrudService<
            VersionedTestEntity,
            VersionedTestDTO,
            Long,
            BaseCrudRepository<VersionedTestEntity, Long>> {

        VersionedTestService(
                BaseCrudRepository<VersionedTestEntity, Long> repository,
                BaseCrudMapper<VersionedTestEntity, VersionedTestDTO> mapper,
                BaseCrudValidator<VersionedTestDTO> validator) {
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
