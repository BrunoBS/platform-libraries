package com.empresa.platform.crud.service;

import com.empresa.platform.crud.dto.BaseCrudDTO;
import com.empresa.platform.crud.mapper.BaseCrudMapper;
import com.empresa.platform.crud.repository.BaseCrudRepository;
import com.empresa.platform.crud.validation.BaseCrudValidator;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.Set;

public abstract class BaseCrudService<
        E,
        D extends BaseCrudDTO<ID, D>,
        ID,
        R extends BaseCrudRepository<E, ID>> {

    private final R repository;
    private final BaseCrudMapper<E, D> mapper;
    private final BaseCrudValidator<D> validator;

    protected BaseCrudService(
            R repository,
            BaseCrudMapper<E, D> mapper,
            BaseCrudValidator<D> validator) {
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
        validateAllowedFilters(resolvedFilters);
        return findAllEntities(resolvedFilters).stream()
                .map(mapper::toDTO)
                .toList();
    }

    protected List<E> findAllEntities(Map<String, String> filters) {
        return repository.findAll();
    }

    protected Set<String> allowedFilters() {
        return Set.of();
    }

    private void validateAllowedFilters(Map<String, String> filters) {
        Set<String> allowed = allowedFilters();

        filters.forEach((name, value) -> {
            if (!allowed.contains(name)) {
                throw unsupportedFilterException(name, value);
            }
        });
    }

    protected RuntimeException unsupportedFilterException(String name, String value) {
        return invalidFilterException(name, value);
    }

    protected boolean booleanFilter(
            Map<String, String> filters,
            String name,
            boolean defaultValue) {

        String value = filters.get(name);
        if (value == null || value.isBlank()) {
            return defaultValue;
        }
        if ("true".equalsIgnoreCase(value)) {
            return true;
        }
        if ("false".equalsIgnoreCase(value)) {
            return false;
        }
        throw invalidFilterException(name, value);
    }

    protected abstract RuntimeException invalidFilterException(String name, String value);

    @Transactional(readOnly = true)
    public D findById(D dto) {
        validator.validateForFind(dto);
        return mapper.toDTO(getEntity(dto));
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
    public D update(D dto) {
        validator.validateForUpdate(dto);
        E entity = getEntity(dto);
        beforeUpdate(entity, dto);
        mapper.updateEntity(entity, dto);
        applyUpdate(entity, dto);
        E saved = repository.save(entity);
        afterUpdate(saved, dto);
        return mapper.toDTO(saved);
    }

    @Transactional
    public void delete(D dto) {
        validator.validateForDelete(dto);
        E entity = getEntity(dto);
        beforeDelete(entity);
        deleteEntity(entity);
        afterDelete(entity);
    }

    protected D normalizeCreate(D dto) {
        return dto == null ? null : dto.withId(null);
    }

    protected E getEntity(D dto) {
        return repository.findById(dto.id())
                .orElseThrow(() -> notFoundException(dto.id()));
    }

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

    protected R repository() {
        return repository;
    }

    protected BaseCrudMapper<E, D> mapper() {
        return mapper;
    }

    protected BaseCrudValidator<D> validator() {
        return validator;
    }
}
