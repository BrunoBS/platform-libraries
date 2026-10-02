package br.com.portalmanager.platform.library.catalog.mapper;

import br.com.portalmanager.platform.library.catalog.dto.CatalogDTO;
import br.com.portalmanager.platform.library.catalog.exception.CatalogTechnicalException;
import br.com.portalmanager.platform.library.catalog.message.CatalogMessageKeys;
import br.com.portalmanager.platform.library.catalog.model.CatalogEntity;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

import java.util.Map;

/** Maps the standard catalog API shape to a catalog entity. */
public final class CatalogMapper<E extends CatalogEntity> {

    private final Class<E> entityClass;
    private final ObjectMapper objectMapper;

    public CatalogMapper(Class<E> entityClass, ObjectMapper objectMapper) {
        this.entityClass = entityClass;
        this.objectMapper = objectMapper;
    }

    public E toEntity(CatalogDTO dto) {
        if (dto == null) return null;
        E entity = createEntity();
        entity.setCode(dto.code());
        updateEntity(entity, dto);
        entity.setActive(true);
        return entity;
    }

    public CatalogDTO toDTO(E entity) {
        if (entity == null) return null;
        return new CatalogDTO(entity.getCode(), entity.getLabel(), entity.getDescription(),
                entity.getSortOrder(), readSettings(entity.getSettings()));
    }

    public void updateEntity(E entity, CatalogDTO dto) {
        if (entity == null || dto == null) return;
        entity.setLabel(dto.label());
        entity.setDescription(dto.description());
        entity.setSortOrder(dto.sortOrder());
        entity.setSettings(dto.settings() == null ? "{}" : dto.settings().toString());
    }

    private E createEntity() {
        try {
            return entityClass.getDeclaredConstructor().newInstance();
        } catch (ReflectiveOperationException exception) {
            throw new CatalogTechnicalException(
                    CatalogMessageKeys.ENTITY_INSTANTIATION_FAILED,
                    Map.of("0", entityClass.getSimpleName()), exception);
        }
    }

    private JsonNode readSettings(String settings) {
        try {
            return settings == null || settings.isBlank()
                    ? objectMapper.createObjectNode()
                    : objectMapper.readTree(settings);
        } catch (JacksonException exception) {
            throw new CatalogTechnicalException(
                    CatalogMessageKeys.SETTINGS_INVALID_STORED_JSON, Map.of(), exception);
        }
    }
}
