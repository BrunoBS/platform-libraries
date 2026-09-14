package com.empresa.platform.catalog.dto;

import tools.jackson.databind.JsonNode;

/**
 * Default DTO for catalogs that only use the standard catalog fields.
 *
 * Consumers only need a dedicated DTO when the catalog exposes additional
 * domain fields beyond the base contract.
 */
public record DefaultCatalogDTO(
        Long id,
        String name,
        String label,
        String description,
        Integer sortOrder,
        JsonNode settings
) implements BaseCatalogDTO<DefaultCatalogDTO> {

    @Override
    public DefaultCatalogDTO withId(Long id) {
        return new DefaultCatalogDTO(id, name, label, description, sortOrder, settings);
    }
}
