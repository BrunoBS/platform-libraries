package br.com.portalmanager.platform.catalog.dto;

import tools.jackson.databind.JsonNode;

public interface BaseCatalogDTO<T extends BaseCatalogDTO<T>> {
    Long id();
    String name();
    String label();
    String description();
    Integer sortOrder();
    JsonNode settings();
    T withId(Long id);
}
