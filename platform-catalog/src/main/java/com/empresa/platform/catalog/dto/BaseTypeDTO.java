package com.empresa.platform.catalog.dto;

import tools.jackson.databind.JsonNode;

public interface BaseTypeDTO<T extends BaseTypeDTO<T, ID>, ID> {
    ID id();
    String name();
    String label();
    String description();
    Integer sortOrder();
    JsonNode settings();
    T withId(ID id);
}
