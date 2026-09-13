package com.empresa.platform.catalog.dto;

import tools.jackson.databind.JsonNode;

public interface BaseCatalogDTO<T extends BaseCatalogDTO<T, ID>, ID> {
    ID id();
    String name();
    String label();
    String description();
    Integer sortOrder();
    JsonNode settings();
    T withId(ID id);
}
