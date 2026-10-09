package br.com.portalmanager.platform.library.catalog.web;

import br.com.portalmanager.platform.library.authorization.model.AuthorizationContext;
import br.com.portalmanager.platform.library.catalog.dto.CatalogDTO;
import br.com.portalmanager.platform.library.catalog.facade.AbstractCatalogFacade;
import br.com.portalmanager.platform.library.catalog.model.CatalogEntity;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/** Reusable REST contract: the controller delegates exclusively to an authorized facade. */
public abstract class CatalogController<E extends CatalogEntity> {

    private final AbstractCatalogFacade<E> facade;

    protected CatalogController(AbstractCatalogFacade<E> facade) {
        this.facade = facade;
    }

    @GetMapping
    public List<CatalogDTO> findAll(AuthorizationContext context,
                                    @RequestParam Map<String, String> filters) {
        return facade.findAll(context, filters);
    }

    @GetMapping("/{code}")
    public CatalogDTO findByCode(AuthorizationContext context, @PathVariable String code) {
        return facade.findByCode(context, code);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public CatalogDTO create(AuthorizationContext context, @RequestBody CatalogDTO dto) {
        return facade.create(context, dto);
    }

    @PutMapping("/{code}")
    public CatalogDTO update(AuthorizationContext context, @PathVariable String code,
                             @RequestBody CatalogDTO dto) {
        return facade.update(context, code, dto);
    }

    @DeleteMapping("/{code}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(AuthorizationContext context, @PathVariable String code) {
        facade.delete(context, code);
    }

    @PostMapping("/{code}/restore")
    public CatalogDTO restore(AuthorizationContext context, @PathVariable String code) {
        return facade.restore(context, code);
    }

}
