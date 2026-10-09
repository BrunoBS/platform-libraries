package br.com.portalmanager.platform.library.catalog.facade;

import br.com.portalmanager.platform.library.authorization.annotation.AuthorizationRequired;
import br.com.portalmanager.platform.library.authorization.model.AuthorizationAction;
import br.com.portalmanager.platform.library.authorization.model.AuthorizationContext;
import br.com.portalmanager.platform.library.authorization.model.AuthorizationLevel;
import br.com.portalmanager.platform.library.catalog.dto.CatalogDTO;
import br.com.portalmanager.platform.library.catalog.model.CatalogEntity;
import br.com.portalmanager.platform.library.catalog.service.AbstractCatalogService;

import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * Public, authorization-protected entry point for consumer catalogs.
 * Overrides must explicitly repeat @AuthorizationRequired so that the
 * authorization policy remains visible on the concrete public method.
 * Calls through this facade must enter through the Spring proxy.
 */
public abstract class AbstractCatalogFacade<E extends CatalogEntity> {

    private final AbstractCatalogService<E> service;

    protected AbstractCatalogFacade(AbstractCatalogService<E> service) {
        this.service = Objects.requireNonNull(service, "service");
    }

    @AuthorizationRequired(level = AuthorizationLevel.OWNER, action = AuthorizationAction.READ)
    public List<CatalogDTO> findAll(AuthorizationContext context, Map<String, String> filters) {
        return service.findAll(filters);
    }

    @AuthorizationRequired(level = AuthorizationLevel.OWNER, action = AuthorizationAction.READ)
    public CatalogDTO findByCode(AuthorizationContext context, String code) {
        return service.findByCode(code);
    }

    @AuthorizationRequired(level = AuthorizationLevel.OWNER, action = AuthorizationAction.CREATE)
    public CatalogDTO create(AuthorizationContext context, CatalogDTO dto) {
        return service.create(dto);
    }

    @AuthorizationRequired(level = AuthorizationLevel.OWNER, action = AuthorizationAction.UPDATE)
    public CatalogDTO update(AuthorizationContext context, String code, CatalogDTO dto) {
        return service.update(code, dto);
    }

    @AuthorizationRequired(level = AuthorizationLevel.OWNER, action = AuthorizationAction.DELETE)
    public void delete(AuthorizationContext context, String code) {
        service.delete(code);
    }

    @AuthorizationRequired(level = AuthorizationLevel.OWNER, action = AuthorizationAction.RESTORE)
    public CatalogDTO restore(AuthorizationContext context, String code) {
        return service.restore(code);
    }
}
