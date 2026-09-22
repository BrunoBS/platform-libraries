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

    @GetMapping("/{code}")
    public D findByCode(@PathVariable String code) {
        return getService().findByCode(code);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public D create(@RequestBody D dto) {
        return getService().create(dto);
    }

    @PutMapping("/{code}")
    public D update(@PathVariable String code, @RequestBody D dto) {
        return getService().update(code, dto);
    }

    @DeleteMapping("/{code}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable String code) {
        getService().delete(code);
    }

    @PostMapping("/{code}/restore")
    public D restore(@PathVariable String code) {
        return getService().restore(code);
    }
}
