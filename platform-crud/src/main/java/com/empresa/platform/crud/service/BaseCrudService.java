package com.empresa.platform.crud.service;

import com.empresa.platform.crud.dto.BaseCrudDTO;
import com.empresa.platform.crud.mapper.BaseCrudMapper;
import com.empresa.platform.crud.repository.BaseCrudRepository;
import com.empresa.platform.crud.validation.BaseCrudValidator;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;

public abstract class BaseCrudService<
        E,
        D extends BaseCrudDTO<ID, D>,
        ID> {

    private final BaseCrudRepository<E, ID> repository;
    private final BaseCrudMapper<E, D> mapper;
    private final BaseCrudValidator<D, ID> validator;

    protected BaseCrudService(
            BaseCrudRepository<E, ID> repository,
            BaseCrudMapper<E, D> mapper,
            BaseCrudValidator<D, ID> validator) {
        this.repository = repository;
        this.mapper = mapper;
        this.validator = validator;
    }

    @Transactional(readOnly = true)
    public List<D> findAll() {
        return findAll(Map.of());
    }

    @Transactional(readOnly = true)
    public List<D> findAll(Map<String, String> filters) {
        Map<String, String> resolvedFilters = filters == null ? Map.of() : Map.copyOf(filters);
        return findAllEntities(resolvedFilters).stream()
                .map(mapper::toDTO)
                .toList();
    }

    protected List<E> findAllEntities(Map<String, String> filters) {
        return repository.findAll();
    }

    @Transactional(readOnly = true)
    public D findById(ID id) {
        return mapper.toDTO(getEntity(id));
    }

    @Transactional
    public D create(D dto) {
        D createDto = normalizeCreate(dto);
        validator.validateForCreate(createDto);
        beforeCreate(createDto);
        E entity = mapper.toEntity(createDto);
        applyCreate(entity, createDto);
        E saved = repository.save(entity);
        afterCreate(saved, createDto);
        return mapper.toDTO(saved);
    }

    @Transactional
    public D update(ID id, D dto) {
        D updateDto = normalizeUpdate(id, dto);
        validator.validateForUpdate(id, updateDto);
        E entity = getEntity(id);
        beforeUpdate(entity, updateDto);
        mapper.updateEntity(entity, updateDto);
        applyUpdate(entity, updateDto);
        E saved = repository.save(entity);
        afterUpdate(saved, updateDto);
        return mapper.toDTO(saved);
    }

    @Transactional
    public void delete(ID id) {
        validator.validateForDelete(id);
        E entity = getEntity(id);
        beforeDelete(entity);
        deleteEntity(entity);
        afterDelete(entity);
    }

    protected D normalizeCreate(D dto) {
        return dto == null ? null : dto.withId(null);
    }

    protected D normalizeUpdate(ID id, D dto) {
        return dto == null ? null : dto.withId(id);
    }

    protected E getEntity(ID id) {
        return repository.findById(id)
                .orElseThrow(() -> notFoundException(id));
    }

    /**
     * Defines how the consuming module represents a missing resource.
     * The CRUD layer detects the condition but does not own domain/application error semantics.
     */
    protected abstract RuntimeException notFoundException(ID id);

    protected void deleteEntity(E entity) {
        repository.delete(entity);
    }

    protected void beforeCreate(D dto) {
    }

    protected void applyCreate(E entity, D dto) {
    }

    protected void afterCreate(E entity, D dto) {
    }

    protected void beforeUpdate(E entity, D dto) {
    }

    protected void applyUpdate(E entity, D dto) {
    }

    protected void afterUpdate(E entity, D dto) {
    }

    protected void beforeDelete(E entity) {
    }

    protected void afterDelete(E entity) {
    }

    protected BaseCrudRepository<E, ID> repository() {
        return repository;
    }

    protected BaseCrudMapper<E, D> mapper() {
        return mapper;
    }

    protected BaseCrudValidator<D, ID> validator() {
        return validator;
    }
}
