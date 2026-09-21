package br.com.portalmanager.platform.catalog.dto;

import tools.jackson.databind.JsonNode;

/**
 * Standard DTO for catalogs that only expose the common catalog fields.
 * Use a dedicated DTO only when the catalog has additional domain fields.
 */
public record CatalogDTO(
        Long id,
        String name,
        String label,
        String description,
        Integer sortOrder,
        JsonNode settings
) implements BaseCatalogDTO<CatalogDTO> {

    @Override
    public CatalogDTO withId(Long id) {
        return new CatalogDTO(id, name, label, description, sortOrder, settings);
    }
}
