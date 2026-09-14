package com.empresa.platform.catalog.web;

import com.empresa.platform.catalog.dto.DefaultCatalogDTO;
import com.empresa.platform.catalog.model.BaseCatalogEntity;
import com.empresa.platform.catalog.service.BaseCatalogService;

/**
 * Convenience controller for catalogs that use {@link DefaultCatalogDTO}.
 */
public abstract class DefaultCatalogController<E extends BaseCatalogEntity>
        extends BaseCatalogController<DefaultCatalogDTO, E> {

    private final BaseCatalogService<E, DefaultCatalogDTO> service;

    protected DefaultCatalogController(BaseCatalogService<E, DefaultCatalogDTO> service) {
        this.service = service;
    }

    @Override
    protected final BaseCatalogService<E, DefaultCatalogDTO> getService() {
        return service;
    }
}
