package com.empresa.platform.catalog.service;

import com.empresa.platform.catalog.dto.BaseCatalogDTO;
import com.empresa.platform.catalog.mapper.BaseCatalogMapper;
import com.empresa.platform.catalog.message.CatalogMessageKeys;
import com.empresa.platform.catalog.model.BaseCatalogEntity;
import com.empresa.platform.catalog.repository.BaseCatalogRepository;
import com.empresa.platform.catalog.validation.BaseCatalogValidator;
import com.empresa.platform.crud.service.BaseListCrudService;
import com.empresa.platform.messaging.exception.NotFoundException;
import com.empresa.platform.messaging.exception.ValidationException;
import com.empresa.platform.messaging.message.PlatformMessageKeys;
import com.empresa.platform.messaging.model.ValidationDetail;
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
        D extends BaseCatalogDTO<D>>
        extends BaseListCrudService<E, D, Long, BaseCatalogRepository<E>> {

    protected BaseCatalogService(
            BaseCatalogRepository<E> repository,
            BaseCatalogMapper<D, E> mapper,
            BaseCatalogValidator<D> validator) {
        super(repository, mapper, validator);
    }

    @Override
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

    @Override
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

        return repository().findAll(specification);
    }

    protected Specification<E> additionalSpecification(Map<String, String> filters) {
        return null;
    }

    @Transactional(readOnly = true)
    public D findById(Long id) {
        return mapper().toDTO(findActiveById(id));
    }

    @Transactional
    public D update(Long id, D dto) {
        return super.update(dto.withId(id));
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

    @Override
    protected E getEntity(D dto) {
        return findActiveById(dto.id());
    }

    private E findActiveById(Long id) {
        return repository().findByIdAndActiveTrue(id)
                .orElseThrow(() -> notFoundException(id));
    }

    @Override
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

    @Override
    protected RuntimeException invalidFilterException(String name, String value) {
        return new ValidationException(
                PlatformMessageKeys.VALIDATION_FAILED,
                List.of(new ValidationDetail(
                        name,
                        PlatformMessageKeys.TYPE_MISMATCH,
                        Map.of("0", name, "1", "boolean")
                ))
        );
    }

    @Override
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

    @Override
    protected void beforeCreate(E entity, D dto) {
        applyAdditionalFields(entity, dto);
        adjustSortOrder(entity, nextSortOrder());
    }

    @Override
    protected void beforeUpdate(E entity, D dto) {
        applyAdditionalFields(entity, dto);
        adjustSortOrder(entity, nextSortOrderExcluding(dto.id()));
    }

    @Override
    protected void deleteEntity(E entity) {
        entity.setActive(false);
        repository().save(entity);
    }

    protected void applyAdditionalFields(E entity, D dto) {
    }

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
