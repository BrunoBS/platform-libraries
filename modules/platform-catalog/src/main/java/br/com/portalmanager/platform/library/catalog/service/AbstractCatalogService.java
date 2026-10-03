package br.com.portalmanager.platform.library.catalog.service;

import br.com.portalmanager.platform.library.catalog.dto.CatalogDTO;
import br.com.portalmanager.platform.library.catalog.mapper.CatalogMapper;
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

import java.util.List;
import java.util.Map;
import java.util.Set;

public abstract sealed class AbstractCatalogService<E extends CatalogEntity>
        permits EnumCatalogService, IncludedCatalogService {

    private static final Set<String> ALLOWED_FILTERS = Set.of("active", "code");

    private final CatalogRepository<E> repository;
    private final CatalogMapper<E> mapper;
    private final AbstractCatalogValidator validator;

    protected AbstractCatalogService(
            CatalogRepository<E> repository,
            CatalogMapper<E> mapper,
            AbstractCatalogValidator validator) {
        this.repository = repository;
        this.mapper = mapper;
        this.validator = validator;
    }

    @Transactional(readOnly = true)
    public List<CatalogDTO> findAll() {
        return findAll(Map.of());
    }

    @Transactional(readOnly = true)
    public List<CatalogDTO> findAll(Map<String, String> filters) {
        Map<String, String> resolved = filters == null ? Map.of() : Map.copyOf(filters);
        validateFilters(resolved);
        return findAllEntities(resolved).stream().map(mapper::toDTO).toList();
    }

    @Transactional
    public CatalogDTO create(CatalogDTO dto) {
        validator.validateForCreate(dto);
        E entity = mapper.toEntity(dto);
        adjustSortOrder(entity, nextSortOrder());
        return mapper.toDTO(repository.save(entity));
    }

    @Transactional(readOnly = true)
    public CatalogDTO findByCode(String code) {
        return mapper.toDTO(findActiveByCode(code));
    }

    @Transactional(readOnly = true)
    public boolean existsActive(String code) {
        return code != null && !code.isBlank() && repository.existsByCodeAndActiveTrue(code);
    }

    @Transactional
    public CatalogDTO update(CatalogDTO dto) {
        validator.validateForUpdate(dto);
        E entity = findActiveByCode(dto.code());
        Integer nextOrder = dto.sortOrder() == null || dto.sortOrder() < 1
                ? nextSortOrderExcluding(dto.code()) : null;
        mapper.updateEntity(entity, dto);
        if (nextOrder != null) entity.setSortOrder(nextOrder);
        return mapper.toDTO(repository.save(entity));
    }

    @Transactional
    public CatalogDTO update(String code, CatalogDTO dto) {
        if (dto == null) {
            return update((CatalogDTO) null);
        }
        return update(dto.withCode(code));
    }

    @Transactional
    public void delete(String code) {
        E entity = findActiveByCode(code);
        validator.validateForDelete(mapper.toDTO(entity));
        entity.setActive(false);
        repository.save(entity);
    }

    @Transactional
    public CatalogDTO restore(String code) {
        E entity = repository.findByCodeAndActiveFalse(code)
                .orElseThrow(() -> restoreException(code));
        validator.validateForUpdate(mapper.toDTO(entity));
        entity.setActive(true);
        return mapper.toDTO(repository.save(entity));
    }

    @Transactional(readOnly = true)
    public List<CatalogDTO> findByCodes(List<String> codes) {
        return repository.findByCodeInAndActiveTrue(codes)
                .stream()
                .map(mapper::toDTO)
                .toList();
    }

    private List<E> findAllEntities(Map<String, String> filters) {
        boolean active = booleanFilter(filters.get("active"));
        String code = filters.get("code");

        Specification<E> specification =
                (root, query, cb) -> cb.equal(root.get("active"), active);

        if (code != null && !code.isBlank()) {
            String contains = "%" + code.toUpperCase() + "%";
            specification = specification.and(
                    (root, query, cb) -> cb.like(cb.upper(root.get("code")), contains));
        }

        return repository.findAll(specification,
                Sort.by(Sort.Order.asc("sortOrder"), Sort.Order.asc("code")));
    }

    private void validateFilters(Map<String, String> filters) {
        filters.keySet().stream()
                .filter(name -> !ALLOWED_FILTERS.contains(name))
                .findFirst()
                .ifPresent(name -> { throw unsupportedFilter(name); });
    }

    private boolean booleanFilter(String value) {
        if (value == null || value.isBlank()) return true;
        if ("true".equalsIgnoreCase(value)) return true;
        if ("false".equalsIgnoreCase(value)) return false;
        throw invalidBooleanFilter("active");
    }

    private E findActiveByCode(String code) {
        return repository.findByCodeAndActiveTrue(code)
                .orElseThrow(() -> new NotFoundException(
                        CatalogMessageKeys.NOT_FOUND,
                        Map.of("0", validator.entityName(), "1", code)));
    }

    private RuntimeException unsupportedFilter(String name) {
        return new ValidationException(
                PlatformMessageKeys.VALIDATION_FAILED,
                List.of(new ValidationDetail(
                        name, CatalogMessageKeys.FILTER_UNSUPPORTED, Map.of("0", name), null)));
    }

    private RuntimeException invalidBooleanFilter(String name) {
        return new ValidationException(
                PlatformMessageKeys.VALIDATION_FAILED,
                List.of(new ValidationDetail(
                        name, PlatformMessageKeys.TYPE_MISMATCH, Map.of("0", name, "1", "boolean"))));
    }

    private RuntimeException restoreException(String code) {
        return new ValidationException(
                CatalogMessageKeys.RESTORE_INVALID,
                Map.of("0", validator.entityName(), "1", code));
    }

    private Integer nextSortOrder() {
        return repository.findFirstByOrderBySortOrderDesc()
                .map(last -> last.getSortOrder() + 1).orElse(1);
    }

    private Integer nextSortOrderExcluding(String code) {
        return repository.findFirstByCodeNotOrderBySortOrderDesc(code)
                .map(last -> last.getSortOrder() + 1).orElse(1);
    }

    private void adjustSortOrder(E entity, Integer nextOrder) {
        if (entity.getSortOrder() == null || entity.getSortOrder() < 1) {
            entity.setSortOrder(nextOrder);
        }
    }
}
