package com.empresa.platform.catalog.mapper;

import com.empresa.platform.catalog.dto.CatalogDTO;
import com.empresa.platform.catalog.model.BaseCatalogEntity;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

/**
 * Mapper for the standard catalog shape.
 */
public class CatalogMapper<E extends BaseCatalogEntity>
        extends BaseCatalogMapper<CatalogDTO, E> {

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
            throw new IllegalStateException(
                    "Invalid catalog settings JSON stored in database",
                    exception
            );
        }
    }
}
