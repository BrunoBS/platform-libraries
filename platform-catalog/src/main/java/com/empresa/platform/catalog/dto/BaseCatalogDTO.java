package com.empresa.platform.catalog.dto;

import com.empresa.platform.crud.dto.BaseCrudDTO;
import tools.jackson.databind.JsonNode;

public interface BaseCatalogDTO<T extends BaseCatalogDTO<T>>
        extends BaseCrudDTO<Long> {

    @Override
    Long id();

    String name();
    String label();
    String description();
    Integer sortOrder();
    JsonNode settings();

    T withId(Long id);
}
