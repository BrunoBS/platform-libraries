package com.empresa.platform.crud.service;

import com.empresa.platform.crud.dto.BaseCrudDTO;
import com.empresa.platform.crud.mapper.BaseCrudMapper;
import com.empresa.platform.crud.repository.BaseCrudRepository;
import com.empresa.platform.crud.validation.BaseCrudValidator;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.Set;

public abstract class BaseListCrudService<
        E,
        D extends BaseCrudDTO<ID>,
        ID,
        R extends BaseCrudRepository<E, ID>>
        extends BaseCrudService<E, D, ID, R> {

    protected BaseListCrudService(
            R repository,
            BaseCrudMapper<E, D> mapper,
            BaseCrudValidator<D> validator) {
        super(repository, mapper, validator);
    }

    @Transactional(readOnly = true)
    public List<D> findAll() {
        return findAll(Map.of());
    }

    @Transactional(readOnly = true)
    public List<D> findAll(Map<String, String> filters) {
        Map<String, String> resolvedFilters =
                filters == null ? Map.of() : Map.copyOf(filters);

        validateAllowedFilters(resolvedFilters);

        return findAllEntities(resolvedFilters)
                .stream()
                .map(mapper()::toDTO)
                .toList();
    }

    protected List<E> findAllEntities(Map<String, String> filters) {
        return repository().findAll();
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

    protected RuntimeException unsupportedFilterException(
            String name,
            String value) {
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

    protected abstract RuntimeException invalidFilterException(
            String name,
            String value);
}
