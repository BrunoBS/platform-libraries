package br.com.portalmanager.platform.catalog.web;

import br.com.portalmanager.platform.catalog.dto.BaseCatalogDTO;
import br.com.portalmanager.platform.catalog.model.BaseCatalogEntity;
import br.com.portalmanager.platform.catalog.service.BaseCatalogService;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

public abstract class BaseCatalogController<
        D extends BaseCatalogDTO<D>,
        E extends BaseCatalogEntity> {

    protected abstract BaseCatalogService<E, D> getService();

    @GetMapping
    public List<D> findAll(@RequestParam Map<String, String> filters) {
        return getService().findAll(filters);
    }

    @GetMapping("/{id}")
    public D findById(@PathVariable Long id) {
        return getService().findById(id);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public D create(@RequestBody D dto) {
        return getService().create(dto);
    }

    @PutMapping("/{id}")
    public D update(@PathVariable Long id, @RequestBody D dto) {
        return getService().update(id, dto);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable Long id) {
        getService().delete(id);
    }

    @PostMapping("/{id}/restore")
    public D restore(@PathVariable Long id) {
        return getService().restore(id);
    }
}
