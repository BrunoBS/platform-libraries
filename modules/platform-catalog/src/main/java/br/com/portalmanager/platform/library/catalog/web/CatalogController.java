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
    public List<CatalogDTO> findAll(@RequestHeader Map<String, String> headers,
                                    @RequestParam Map<String, String> filters) {
        return facade.findAll(authorizationContext(headers), filters);
    }

    @GetMapping("/{code}")
    public CatalogDTO findByCode(@RequestHeader Map<String, String> headers, @PathVariable String code) {
        return facade.findByCode(authorizationContext(headers), code);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public CatalogDTO create(@RequestHeader Map<String, String> headers, @RequestBody CatalogDTO dto) {
        return facade.create(authorizationContext(headers), dto);
    }

    @PutMapping("/{code}")
    public CatalogDTO update(@RequestHeader Map<String, String> headers, @PathVariable String code,
                             @RequestBody CatalogDTO dto) {
        return facade.update(authorizationContext(headers), code, dto);
    }

    @DeleteMapping("/{code}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@RequestHeader Map<String, String> headers, @PathVariable String code) {
        facade.delete(authorizationContext(headers), code);
    }

    @PostMapping("/{code}/restore")
    public CatalogDTO restore(@RequestHeader Map<String, String> headers, @PathVariable String code) {
        return facade.restore(authorizationContext(headers), code);
    }

    private static AuthorizationContext authorizationContext(Map<String, String> headers) {
        return new AuthorizationContext(
                header(headers, "correlation-id", "correlationid"),
                header(headers, "authorization"),
                header(headers, "workspace-identifier", "workspaceidentifier"),
                header(headers, "environment-identifier", "environmentidentifier"),
                header(headers, "application-identifier", "applicationidentifier"),
                header(headers, "x-forwarded-for"),
                header(headers, "user-agent"),
                null);
    }

    private static String header(Map<String, String> headers, String... names) {
        for (var entry : headers.entrySet()) {
            for (String name : names) {
                if (entry.getKey().equalsIgnoreCase(name)) return entry.getValue();
            }
        }
        return null;
    }
}
