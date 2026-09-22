package br.com.portalmanager.platform.catalog.dto;

import tools.jackson.databind.JsonNode;

public interface CatalogDTOContract<T extends CatalogDTOContract<T>> {
    String code();
    String label();
    String description();
    Integer sortOrder();
    JsonNode settings();
    T withCode(String code);
}
