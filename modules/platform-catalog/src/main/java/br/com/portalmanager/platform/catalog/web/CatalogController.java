package br.com.portalmanager.platform.catalog.web;

import br.com.portalmanager.platform.catalog.dto.CatalogDTO;
import br.com.portalmanager.platform.catalog.model.CatalogEntity;
import br.com.portalmanager.platform.catalog.service.AbstractCatalogService;

/**
 * Controller base for catalogs that use the standard {@link CatalogDTO} shape.
 */
public abstract class CatalogController<E extends CatalogEntity>
        extends AbstractCatalogController<CatalogDTO, E> {

    private final AbstractCatalogService<E, CatalogDTO> service;

    protected CatalogController(AbstractCatalogService<E, CatalogDTO> service) {
        this.service = service;
    }

    @Override
    protected final AbstractCatalogService<E, CatalogDTO> getService() {
        return service;
    }
}
