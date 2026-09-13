package com.empresa.platform.catalog.mapper;

import com.empresa.platform.catalog.dto.BaseTypeDTO;
import com.empresa.platform.catalog.model.BaseType;

public abstract class BaseTypeMapper<D extends BaseTypeDTO<D, ID>, E extends BaseType, ID>
        implements BaseMapper<D, E> {

    private final Class<E> entityClass;

    protected BaseTypeMapper(Class<E> entityClass) {
        this.entityClass = entityClass;
    }

    @Override
    public E toEntity(D dto) {
        if (dto == null) {
            return null;
        }
        E entity = createEntityInstance();
        mapCommonFields(entity, dto);
        return entity;
    }

    @Override
    public void updateEntity(E entity, D dto) {
        if (entity != null && dto != null) {
            mapCommonFields(entity, dto);
        }
    }

    protected void mapCommonFields(E entity, D dto) {
        entity.setName(dto.name());
        entity.setLabel(dto.label());
        entity.setDescription(dto.description());
        entity.setSortOrder(dto.sortOrder());
        entity.setSettings(dto.settings() == null ? "{}" : dto.settings().toString());
        entity.setActive(true);
    }

    private E createEntityInstance() {
        try {
            return entityClass.getDeclaredConstructor().newInstance();
        } catch (ReflectiveOperationException exception) {
            throw new IllegalStateException("Unable to instantiate catalog entity " + entityClass.getSimpleName(), exception);
        }
    }
}
