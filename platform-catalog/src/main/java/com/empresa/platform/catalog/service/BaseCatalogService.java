package com.empresa.platform.catalog.service;

import com.empresa.platform.catalog.dto.BaseCatalogDTO;
import com.empresa.platform.catalog.mapper.BaseMapper;
import com.empresa.platform.catalog.message.CatalogMessageKeys;
import com.empresa.platform.catalog.model.BaseCatalogEntity;
import com.empresa.platform.catalog.repository.BaseCatalogRepository;
import com.empresa.platform.catalog.validation.BaseValidator;
import com.empresa.platform.messaging.exception.NotFoundException;
import com.empresa.platform.messaging.exception.ValidationException;

import java.util.List;
import java.util.Map;

public abstract class BaseCatalogService<E extends BaseCatalogEntity, D extends BaseCatalogDTO<D>> {
    protected final BaseCatalogRepository<E> repository;
    protected final BaseMapper<D, E> mapper;
    protected final BaseValidator<D> validator;

    protected BaseCatalogService(BaseCatalogRepository<E> repository, BaseMapper<D, E> mapper, BaseValidator<D> validator) {
        this.repository = repository;
        this.mapper = mapper;
        this.validator = validator;
    }

    public List<D> findAll(boolean active, String name, Map<String, String> filters) {
        return repository.findByActive(active).stream()
                .map(mapper::toDTO)
                .filter(dto -> name == null || dto.name().contains(name))
                .filter(dto -> matchesAdditionalFilters(dto, filters))
                .toList();
    }

    protected boolean matchesAdditionalFilters(D dto, Map<String, String> filters) { return true; }
    public D findById(Long id) { return mapper.toDTO(getEntity(id)); }
    public E findByName(String name) { return repository.findByNameAndActiveTrue(name).orElseThrow(() -> new NotFoundException(CatalogMessageKeys.NOT_FOUND, Map.of("0", validator.entityName()))); }

    public D create(D dto) {
        validator.validateForCreate(dto);
        E entity = mapper.toEntity(dto);
        applyAdditionalFields(entity, dto);
        adjustSortOrder(entity, nextSortOrder());
        return mapper.toDTO(repository.save(entity));
    }

    public D update(D dto) {
        validator.validateForUpdate(dto);
        E entity = getEntity(dto.id());
        mapper.updateEntity(entity, dto);
        applyAdditionalFields(entity, dto);
        adjustSortOrder(entity, nextSortOrderExcluding(dto.id()));
        return mapper.toDTO(repository.save(entity));
    }

    public void delete(Long id) { validator.validateForDelete(id); E entity = getEntity(id); entity.setActive(false); repository.save(entity); }
    public D restore(Long id) { E entity = repository.findByIdAndActiveFalse(id).orElseThrow(() -> new ValidationException(CatalogMessageKeys.RESTORE_INVALID, Map.of("0", validator.entityName()))); entity.setActive(true); return mapper.toDTO(repository.save(entity)); }
    public List<E> findByNames(List<String> names) { return repository.findByNameInAndActiveTrue(names); }
    protected void applyAdditionalFields(E entity, D dto) { }
    protected E getEntity(Long id) { return repository.findByIdAndActiveTrue(id).orElseThrow(() -> new NotFoundException(CatalogMessageKeys.NOT_FOUND, Map.of("0", validator.entityName()))); }
    private Integer nextSortOrder() { return repository.findFirstByOrderBySortOrderDesc().map(last -> last.getSortOrder() + 1).orElse(1); }
    private Integer nextSortOrderExcluding(Long id) { return repository.findFirstByIdNotOrderBySortOrderDesc(id).map(last -> last.getSortOrder() + 1).orElse(1); }
    private void adjustSortOrder(E entity, Integer nextOrder) { if (entity.getSortOrder() == null || entity.getSortOrder() < 1) entity.setSortOrder(nextOrder); }
}
