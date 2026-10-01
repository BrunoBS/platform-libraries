package br.com.portalmanager.platform.library.catalog.mapper;

import br.com.portalmanager.platform.library.catalog.dto.CatalogDTO;
import br.com.portalmanager.platform.library.catalog.model.CatalogEntity;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

/**
 * Mapper for the standard catalog shape.
 */
public class CatalogMapper<E extends CatalogEntity>
        extends AbstractCatalogMapper<CatalogDTO, E> {

    private final ObjectMapper objectMapper;

    public CatalogMapper(Class<E> entityClass, ObjectMapper objectMapper) {
        super(entityClass);
        this.objectMapper = objectMapper;
    }

    @Override
    public CatalogDTO toDTO(E entity) {
        if (entity == null) {
            return null;
        }

        return new CatalogDTO(
                entity.getCode(),
                entity.getLabel(),
                entity.getDescription(),
                entity.getSortOrder(),
                entity.getSettings()
        );
    }

}
