package br.com.portalmanager.platform.library.catalog.web;

import br.com.portalmanager.platform.library.catalog.dto.CatalogDTO;
import br.com.portalmanager.platform.library.catalog.model.CatalogEntity;
import br.com.portalmanager.platform.library.catalog.service.AbstractCatalogService;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/** Reusable REST contract for the standard catalog shape. */
public abstract class CatalogController<E extends CatalogEntity> {

    private final AbstractCatalogService<E> service;

    protected CatalogController(AbstractCatalogService<E> service) {
        this.service = service;
    }

    @GetMapping
    public List<CatalogDTO> findAll(@RequestParam Map<String, String> filters) {
        return service.findAll(filters);
    }

    @GetMapping("/{code}")
    public CatalogDTO findByCode(@PathVariable String code) {
        return service.findByCode(code);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public CatalogDTO create(@RequestBody CatalogDTO dto) {
        return service.create(dto);
    }

    @PutMapping("/{code}")
    public CatalogDTO update(@PathVariable String code, @RequestBody CatalogDTO dto) {
        return service.update(code, dto);
    }

    @DeleteMapping("/{code}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable String code) {
        service.delete(code);
    }

    @PostMapping("/{code}/restore")
    public CatalogDTO restore(@PathVariable String code) {
        return service.restore(code);
    }
}
