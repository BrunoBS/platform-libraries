package br.com.portalmanager.platform.library.catalog.mapper;

import br.com.portalmanager.platform.library.catalog.dto.CatalogDTO;
import br.com.portalmanager.platform.library.catalog.exception.CatalogTechnicalException;
import br.com.portalmanager.platform.library.catalog.message.CatalogMessageKeys;
import br.com.portalmanager.platform.library.catalog.model.CatalogEntity;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

import java.util.Map;

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
