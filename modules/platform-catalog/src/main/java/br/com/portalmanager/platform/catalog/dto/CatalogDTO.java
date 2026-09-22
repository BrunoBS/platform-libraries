package br.com.portalmanager.platform.catalog.dto;

import tools.jackson.databind.JsonNode;

/**
 * Standard DTO for persisted catalogs identified by an immutable semantic code.
 */
public record CatalogDTO(
        String code,
        String label,
        String description,
        Integer sortOrder,
        JsonNode settings
) implements BaseCatalogDTO<CatalogDTO> {

    @Override
    public CatalogDTO withCode(String code) {
        return new CatalogDTO(code, label, description, sortOrder, settings);
    }
}
