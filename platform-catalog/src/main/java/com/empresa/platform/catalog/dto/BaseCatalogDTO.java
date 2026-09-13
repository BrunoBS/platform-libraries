package com.empresa.platform.catalog.dto;

import com.empresa.platform.crud.dto.BaseCrudDTO;
import tools.jackson.databind.JsonNode;

public interface BaseCatalogDTO<T extends BaseCatalogDTO<T>>
        extends BaseCrudDTO<Long, T> {

    @Override
    Long id();

    String name();
    String label();
    String description();
    Integer sortOrder();
    JsonNode settings();

    @Override
    T withId(Long id);
}
