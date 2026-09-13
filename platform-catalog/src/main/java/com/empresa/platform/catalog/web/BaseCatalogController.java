package com.empresa.platform.catalog.web;

import com.empresa.platform.catalog.dto.BaseCatalogDTO;
import com.empresa.platform.catalog.model.BaseCatalogEntity;
import com.empresa.platform.catalog.service.BaseCatalogService;
import com.empresa.platform.crud.service.BaseCrudService;
import com.empresa.platform.crud.web.CrudControllerSupport;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public abstract class BaseCatalogController<D extends BaseCatalogDTO<D>, E extends BaseCatalogEntity>
        extends CrudControllerSupport<E, D, Long> {

    protected abstract BaseCatalogService<E, D> getService();

    @Override
    protected final BaseCrudService<E, D, Long> service() {
        return getService();
    }

    @GetMapping
    public ResponseEntity<List<D>> findAll(
            @RequestParam(name = "active", defaultValue = "true") boolean active,
            @RequestParam(name = "name", required = false) String name,
            @RequestParam Map<String, String> requestParams) {
        Map<String, String> additionalFilters = new HashMap<>(requestParams);
        additionalFilters.remove("active");
        additionalFilters.remove("name");
        return ResponseEntity.ok(getService().findAll(active, name, Map.copyOf(additionalFilters)));
    }

    @GetMapping("/{id}")
    public ResponseEntity<D> findById(@PathVariable Long id) {
        return ResponseEntity.ok(findByIdInternal(id));
    }

    @PostMapping
    public ResponseEntity<List<D>> create(@RequestBody List<D> dtos) {
        return ResponseEntity.ok(dtos.stream().map(this::createInternal).toList());
    }

    @PutMapping("/{id}")
    public ResponseEntity<D> update(@PathVariable Long id, @RequestBody D dto) {
        return ResponseEntity.ok(updateInternal(id, dto));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        deleteInternal(id);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/{id}/restore")
    public ResponseEntity<D> restore(@PathVariable Long id) {
        return ResponseEntity.ok(getService().restore(id));
    }
}
