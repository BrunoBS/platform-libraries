package br.com.portalmanager.platform.library.catalog.service;

import br.com.portalmanager.platform.library.catalog.dto.CatalogDTOContract;
import br.com.portalmanager.platform.library.catalog.mapper.AbstractCatalogMapper;
import br.com.portalmanager.platform.library.catalog.message.CatalogMessageKeys;
import br.com.portalmanager.platform.library.catalog.model.CatalogEntity;
import br.com.portalmanager.platform.library.catalog.repository.CatalogRepository;
import br.com.portalmanager.platform.library.catalog.validation.AbstractCatalogValidator;
import br.com.portalmanager.platform.library.messaging.exception.NotFoundException;
import br.com.portalmanager.platform.library.messaging.exception.ValidationException;
import br.com.portalmanager.platform.library.messaging.message.PlatformMessageKeys;
import br.com.portalmanager.platform.library.messaging.model.ValidationDetail;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public abstract sealed class AbstractCatalogService<
        E extends CatalogEntity,
        D extends CatalogDTOContract<D>> permits EnumCatalogService, DynamicCatalogService {

    private final CatalogRepository<E> repository;
    private final AbstractCatalogMapper<D, E> mapper;
    private final AbstractCatalogValidator<D> validator;

    protected AbstractCatalogService(
            CatalogRepository<E> repository,
            AbstractCatalogMapper<D, E> mapper,
            AbstractCatalogValidator<D> validator) {
        this.repository = repository;
        this.mapper = mapper;
        this.validator = validator;
    }

    protected Set<String> allowedFilters() {
        return Stream.concat(
                        Stream.of("active", "code"),
                        additionalAllowedFilters().stream()
                )
                .collect(Collectors.toUnmodifiableSet());
    }

    protected Set<String> additionalAllowedFilters() {
        return Set.of();
    }

    @Transactional(readOnly = true)
    public List<D> findAll() {
        return findAll(Map.of());
    }

    @Transactional(readOnly = true)
    public List<D> findAll(Map<String, String> filters) {
        Map<String, String> resolved = filters == null ? Map.of() : Map.copyOf(filters);
        validateAllowedFilters(resolved);
        return findAllEntities(resolved).stream().map(mapper::toDTO).toList();
    }

    @Transactional
    public D create(D dto) {
        validator.validateForCreate(dto);
        E entity = mapper.toEntity(dto);
        beforeCreate(entity, dto);
        E saved = repository.save(entity);
        afterCreate(saved, dto);
        return mapper.toDTO(saved);
    }

    private void validateAllowedFilters(Map<String, String> filters) {
        Set<String> allowed = allowedFilters();
        filters.forEach((name, value) -> {
            if (!allowed.contains(name)) {
                throw unsupportedFilterException(name, value);
            }
        });
    }

    protected boolean booleanFilter(Map<String, String> filters, String name, boolean defaultValue) {
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

    protected List<E> findAllEntities(Map<String, String> filters) {
        boolean active = booleanFilter(filters, "active", true);
        String code = filters.get("code");

        Map<String, String> additionalFilters = new HashMap<>(filters);
        additionalFilters.remove("active");
        additionalFilters.remove("code");

        Specification<E> specification =
                (root, query, cb) -> cb.equal(root.get("active"), active);

        if (code != null && !code.isBlank()) {
            String contains = "%" + code.toUpperCase() + "%";
            specification = specification.and(
                    (root, query, cb) -> cb.like(cb.upper(root.get("code")), contains)
            );
        }

        Specification<E> additional = additionalSpecification(Map.copyOf(additionalFilters));
        if (additional != null) {
            specification = specification.and(additional);
        }

        return repository().findAll(
                specification,
                Sort.by(Sort.Order.asc("sortOrder"), Sort.Order.asc("code"))
        );
    }

    protected Specification<E> additionalSpecification(Map<String, String> filters) {
        return null;
    }

    @Transactional(readOnly = true)
    public D findByCode(String code) {
        return mapper().toDTO(findActiveByCode(code));
    }

    @Transactional(readOnly = true)
    public boolean existsActive(String code) {
        return code != null
                && !code.isBlank()
                && repository().existsByCodeAndActiveTrue(code);
    }

    @Transactional
    public D update(D dto) {
        validator().validateForUpdate(dto);

        E entity = getEntity(dto);
        Integer nextOrder = dto.sortOrder() == null || dto.sortOrder() < 1
                ? nextSortOrderExcluding(dto.code())
                : null;

        mapper().updateEntity(entity, dto);
        applyAdditionalFields(entity, dto);

        if (nextOrder != null) {
            entity.setSortOrder(nextOrder);
        }

        E saved = repository().save(entity);
        afterUpdate(saved, dto);
        return mapper().toDTO(saved);
    }

    @Transactional
    public D update(String code, D dto) {
        return update(dto.withCode(code));
    }

    @Transactional
    public void delete(String code) {
        E entity = findActiveByCode(code);
        D dto = mapper().toDTO(entity);
        validator().validateForDelete(dto);
        beforeDelete(entity);
        deleteEntity(entity);
        afterDelete(entity);
    }

    @Transactional
    public D restore(String code) {
        E entity = repository().findByCodeAndActiveFalse(code)
                .orElseThrow(() -> restoreException(code));

        validator().validateForUpdate(mapper().toDTO(entity));
        entity.setActive(true);
        return mapper().toDTO(repository().save(entity));
    }

    @Transactional(readOnly = true)
    public List<E> findByCodes(List<String> codes) {
        return repository().findByCodeInAndActiveTrue(codes);
    }

    protected E getEntity(D dto) {
        return findActiveByCode(dto.code());
    }

    private E findActiveByCode(String code) {
        return repository().findByCodeAndActiveTrue(code)
                .orElseThrow(() -> notFoundException(code));
    }

    protected RuntimeException unsupportedFilterException(String name, String value) {
        return new ValidationException(
                PlatformMessageKeys.VALIDATION_FAILED,
                List.of(new ValidationDetail(
                        name,
                        CatalogMessageKeys.FILTER_UNSUPPORTED,
                        Map.of("0", name),
                        null
                ))
        );
    }

    protected RuntimeException invalidFilterException(String name, String value) {
        return invalidFilterException(name, value, "boolean");
    }

    protected RuntimeException invalidFilterException(
            String name,
            String value,
            String expectedType) {
        return new ValidationException(
                PlatformMessageKeys.VALIDATION_FAILED,
                List.of(new ValidationDetail(
                        name,
                        PlatformMessageKeys.TYPE_MISMATCH,
                        Map.of("0", name, "1", expectedType)
                ))
        );
    }

    protected RuntimeException notFoundException(String code) {
        return new NotFoundException(
                CatalogMessageKeys.NOT_FOUND,
                Map.of("0", validator().entityName(), "1", code)
        );
    }

    protected RuntimeException restoreException(String code) {
        return new ValidationException(
                CatalogMessageKeys.RESTORE_INVALID,
                Map.of("0", validator().entityName(), "1", code)
        );
    }

    protected void beforeCreate(E entity, D dto) {
        applyAdditionalFields(entity, dto);
        adjustSortOrder(entity, nextSortOrder());
    }

    protected void deleteEntity(E entity) {
        entity.setActive(false);
        repository().save(entity);
    }

    protected void applyAdditionalFields(E entity, D dto) {}
    protected void afterCreate(E entity, D dto) {}
    protected void afterUpdate(E entity, D dto) {}
    protected void beforeDelete(E entity) {}
    protected void afterDelete(E entity) {}
    protected CatalogRepository<E> repository() { return repository; }
    protected AbstractCatalogMapper<D, E> mapper() { return mapper; }
    protected AbstractCatalogValidator<D> validator() { return validator; }

    private Integer nextSortOrder() {
        return repository().findFirstByOrderBySortOrderDesc()
                .map(last -> last.getSortOrder() + 1)
                .orElse(1);
    }

    private Integer nextSortOrderExcluding(String code) {
        return repository().findFirstByCodeNotOrderBySortOrderDesc(code)
                .map(last -> last.getSortOrder() + 1)
                .orElse(1);
    }

    private void adjustSortOrder(E entity, Integer nextOrder) {
        if (entity.getSortOrder() == null || entity.getSortOrder() < 1) {
            entity.setSortOrder(nextOrder);
        }
    }
}
