package br.com.portalmanager.core.catalog.service;

import br.com.portalmanager.core.catalog.dto.BaseCatalogDTO;
import br.com.portalmanager.core.catalog.mapper.BaseCatalogMapper;
import br.com.portalmanager.core.catalog.message.CatalogMessageKeys;
import br.com.portalmanager.core.catalog.model.BaseCatalogEntity;
import br.com.portalmanager.core.catalog.repository.BaseCatalogRepository;
import br.com.portalmanager.core.catalog.validation.BaseCatalogValidator;
import br.com.portalmanager.core.messaging.exception.NotFoundException;
import br.com.portalmanager.core.messaging.exception.ValidationException;
import br.com.portalmanager.core.messaging.message.PlatformMessageKeys;
import br.com.portalmanager.core.messaging.model.ValidationDetail;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public abstract class BaseCatalogService<
        E extends BaseCatalogEntity,
        D extends BaseCatalogDTO<D>> {

    private final BaseCatalogRepository<E> repository;
    private final BaseCatalogMapper<D, E> mapper;
    private final BaseCatalogValidator<D> validator;

    protected BaseCatalogService(
            BaseCatalogRepository<E> repository,
            BaseCatalogMapper<D, E> mapper,
            BaseCatalogValidator<D> validator) {
        this.repository = repository;
        this.mapper = mapper;
        this.validator = validator;
    }

    protected Set<String> allowedFilters() {
        return Stream.concat(
                        Stream.of("active", "name"),
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
            if (!allowed.contains(name)) throw unsupportedFilterException(name, value);
        });
    }

    protected boolean booleanFilter(Map<String, String> filters, String name, boolean defaultValue) {
        String value = filters.get(name);
        if (value == null || value.isBlank()) return defaultValue;
        if ("true".equalsIgnoreCase(value)) return true;
        if ("false".equalsIgnoreCase(value)) return false;
        throw invalidFilterException(name, value);
    }

    protected List<E> findAllEntities(Map<String, String> filters) {
        boolean active = booleanFilter(filters, "active", true);
        String name = filters.get("name");

        Map<String, String> additionalFilters = new HashMap<>(filters);
        additionalFilters.remove("active");
        additionalFilters.remove("name");

        Specification<E> specification =
                (root, query, cb) -> cb.equal(root.get("active"), active);

        if (name != null && !name.isBlank()) {
            String contains = "%" + name.toLowerCase() + "%";
            specification = specification.and(
                    (root, query, cb) -> cb.like(cb.lower(root.get("name")), contains)
            );
        }

        Specification<E> additional =
                additionalSpecification(Map.copyOf(additionalFilters));

        if (additional != null) {
            specification = specification.and(additional);
        }

        return repository().findAll(
                specification,
                Sort.by(Sort.Order.asc("sortOrder"), Sort.Order.asc("id"))
        );
    }

    protected Specification<E> additionalSpecification(Map<String, String> filters) {
        return null;
    }

    @Transactional(readOnly = true)
    public D findById(Long id) {
        return mapper().toDTO(findActiveById(id));
    }

    @Transactional
    public D update(D dto) {
        validator().validateForUpdate(dto);

        E entity = getEntity(dto);
        Integer nextOrder = dto.sortOrder() == null || dto.sortOrder() < 1
                ? nextSortOrderExcluding(dto.id())
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
    public D update(Long id, D dto) {
        return update(dto.withId(id));
    }

    @Transactional
    public void delete(Long id) {
        E entity = findActiveById(id);
        D dto = mapper().toDTO(entity);
        validator().validateForDelete(dto);
        beforeDelete(entity);
        deleteEntity(entity);
        afterDelete(entity);
    }

    @Transactional(readOnly = true)
    public E findByName(String name) {
        return repository().findByNameAndActiveTrue(name)
                .orElseThrow(() -> notFoundException(null));
    }

    @Transactional
    public D restore(Long id) {
        E entity = repository().findByIdAndActiveFalse(id)
                .orElseThrow(() -> restoreException(id));

        validator().validateForUpdate(mapper().toDTO(entity));
        entity.setActive(true);
        return mapper().toDTO(repository().save(entity));
    }

    @Transactional(readOnly = true)
    public List<E> findByNames(List<String> names) {
        return repository().findByNameInAndActiveTrue(names);
    }

    protected E getEntity(D dto) {
        return findActiveById(dto.id());
    }

    private E findActiveById(Long id) {
        return repository().findByIdAndActiveTrue(id)
                .orElseThrow(() -> notFoundException(id));
    }

    protected RuntimeException unsupportedFilterException(String name, String value) {
        return new ValidationException(
                PlatformMessageKeys.VALIDATION_FAILED,
                List.of(new ValidationDetail(
                        name,
                        null,
                        Map.of(),
                        "Unsupported filter: " + name
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

    protected RuntimeException notFoundException(Long id) {
        return new NotFoundException(
                CatalogMessageKeys.NOT_FOUND,
                Map.of("0", validator().entityName()));
    }

    protected RuntimeException restoreException(Long id) {
        return new ValidationException(
                CatalogMessageKeys.RESTORE_INVALID,
                Map.of("0", validator().entityName(), "1", id));
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
    protected BaseCatalogRepository<E> repository() { return repository; }
    protected BaseCatalogMapper<D, E> mapper() { return mapper; }
    protected BaseCatalogValidator<D> validator() { return validator; }

    private Integer nextSortOrder() {
        return repository().findFirstByOrderBySortOrderDesc()
                .map(last -> last.getSortOrder() + 1)
                .orElse(1);
    }

    private Integer nextSortOrderExcluding(Long id) {
        return repository().findFirstByIdNotOrderBySortOrderDesc(id)
                .map(last -> last.getSortOrder() + 1)
                .orElse(1);
    }

    private void adjustSortOrder(E entity, Integer nextOrder) {
        if (entity.getSortOrder() == null || entity.getSortOrder() < 1) {
            entity.setSortOrder(nextOrder);
        }
    }
}
