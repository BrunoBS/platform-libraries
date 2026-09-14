package com.empresa.platform.catalog.web;

import com.empresa.platform.catalog.dto.CatalogDTO;
import com.empresa.platform.catalog.model.BaseCatalogEntity;
import com.empresa.platform.catalog.service.BaseCatalogService;

/**
 * Controller base for catalogs that use the standard {@link CatalogDTO} shape.
 */
public abstract class CatalogController<E extends BaseCatalogEntity>
        extends BaseCatalogController<CatalogDTO, E> {

    private final BaseCatalogService<E, CatalogDTO> service;

    protected CatalogController(BaseCatalogService<E, CatalogDTO> service) {
        this.service = service;
    }

    @Override
    protected final BaseCatalogService<E, CatalogDTO> getService() {
        return service;
    }
}
