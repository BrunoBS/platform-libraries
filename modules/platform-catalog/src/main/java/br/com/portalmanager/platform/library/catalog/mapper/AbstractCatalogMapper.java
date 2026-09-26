package br.com.portalmanager.platform.library.catalog.mapper;

import br.com.portalmanager.platform.library.catalog.dto.CatalogDTOContract;
import br.com.portalmanager.platform.library.catalog.model.CatalogEntity;

public abstract class AbstractCatalogMapper<D extends CatalogDTOContract<D>, E extends CatalogEntity> {

    private final Class<E> entityClass;

    protected AbstractCatalogMapper(Class<E> entityClass) {
        this.entityClass = entityClass;
    }

    public E toEntity(D dto) {
        if (dto == null) {
            return null;
        }
        E entity = createEntityInstance();
        entity.setCode(dto.code());
        mapMutableFields(entity, dto);
        entity.setActive(true);
        return entity;
    }

    public abstract D toDTO(E entity);

    public void updateEntity(E entity, D dto) {
        if (entity != null && dto != null) {
            mapMutableFields(entity, dto);
        }
    }

    protected void mapMutableFields(E entity, D dto) {
        entity.setLabel(dto.label());
        entity.setDescription(dto.description());
        entity.setSortOrder(dto.sortOrder());
        entity.setSettings(dto.settings() == null ? "{}" : dto.settings().toString());
    }

    private E createEntityInstance() {
        try {
            return entityClass.getDeclaredConstructor().newInstance();
        } catch (ReflectiveOperationException exception) {
            throw new IllegalStateException(
                    "Unable to instantiate catalog entity " + entityClass.getSimpleName(),
                    exception
            );
        }
    }
}
