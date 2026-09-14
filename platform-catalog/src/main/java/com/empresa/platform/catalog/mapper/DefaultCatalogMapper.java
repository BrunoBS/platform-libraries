package com.empresa.platform.catalog.mapper;

import com.empresa.platform.catalog.dto.DefaultCatalogDTO;
import com.empresa.platform.catalog.model.BaseCatalogEntity;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

/**
 * Default mapper for catalogs that only expose the standard catalog fields.
 */
public class DefaultCatalogMapper<E extends BaseCatalogEntity>
        extends BaseCatalogMapper<DefaultCatalogDTO, E> {

    private final ObjectMapper objectMapper;

    public DefaultCatalogMapper(Class<E> entityClass, ObjectMapper objectMapper) {
        super(entityClass);
        this.objectMapper = objectMapper;
    }

    @Override
    public DefaultCatalogDTO toDTO(E entity) {
        if (entity == null) {
            return null;
        }

        return new DefaultCatalogDTO(
                entity.getId(),
                entity.getName(),
                entity.getLabel(),
                entity.getDescription(),
                entity.getSortOrder(),
                readSettings(entity.getSettings())
        );
    }

    private JsonNode readSettings(String settings) {
        try {
            return settings == null || settings.isBlank()
                    ? objectMapper.createObjectNode()
                    : objectMapper.readTree(settings);
        } catch (JacksonException exception) {
            return objectMapper.createObjectNode();
        }
    }
}
