package com.empresa.platform.catalog.mapper;

import com.empresa.platform.catalog.dto.BaseCatalogDTO;
import com.empresa.platform.catalog.model.BaseCatalogEntity;

public abstract class BaseCatalogMapper<D extends BaseCatalogDTO<D>, E extends BaseCatalogEntity> {
    private final Class<E> entityClass;
    protected BaseCatalogMapper(Class<E> entityClass) { this.entityClass = entityClass; }
    public E toEntity(D dto) {
        if (dto == null) return null;
        E entity = createEntityInstance();
        mapCommonFields(entity, dto);
        entity.setActive(true);
        return entity;
    }
    public abstract D toDTO(E entity);
    public void updateEntity(E entity, D dto) {
        if (entity != null && dto != null) mapCommonFields(entity, dto);
    }
    protected void mapCommonFields(E entity, D dto) {
        entity.setName(dto.name());
        entity.setLabel(dto.label());
        entity.setDescription(dto.description());
        entity.setSortOrder(dto.sortOrder());
        entity.setSettings(dto.settings() == null ? "{}" : dto.settings().toString());
    }
    private E createEntityInstance() {
        try { return entityClass.getDeclaredConstructor().newInstance(); }
        catch (ReflectiveOperationException exception) {
            throw new IllegalStateException("Unable to instantiate catalog entity " + entityClass.getSimpleName(), exception);
        }
    }
}
