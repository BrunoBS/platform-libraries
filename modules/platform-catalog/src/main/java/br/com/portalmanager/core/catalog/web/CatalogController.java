package br.com.portalmanager.core.catalog.web;

import br.com.portalmanager.core.catalog.dto.CatalogDTO;
import br.com.portalmanager.core.catalog.model.BaseCatalogEntity;
import br.com.portalmanager.core.catalog.service.BaseCatalogService;

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
