package br.com.portalmanager.platform.library.catalog.dto;

import tools.jackson.databind.JsonNode;

/** Standard API shape for every managed catalog. */
public record CatalogDTO(
        String code,
        String label,
        String description,
        Integer sortOrder,
        JsonNode settings
) {
    public CatalogDTO withCode(String code) {
        return new CatalogDTO(code, label, description, sortOrder, settings);
    }
}
