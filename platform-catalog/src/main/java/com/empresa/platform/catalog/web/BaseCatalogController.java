package com.empresa.platform.catalog.web;

import com.empresa.platform.catalog.dto.BaseCatalogDTO;
import com.empresa.platform.catalog.model.BaseCatalogEntity;
import com.empresa.platform.catalog.service.BaseCatalogService;
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

public abstract class BaseCatalogController<D extends BaseCatalogDTO<D, ID>, E extends BaseCatalogEntity, ID> {

    protected abstract BaseCatalogService<E, D, ID> getService();

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
    public ResponseEntity<D> findById(@PathVariable ID id) { return ResponseEntity.ok(getService().findById(id)); }

    @PostMapping
    public ResponseEntity<List<D>> create(@RequestBody List<D> dtos) {
        return ResponseEntity.ok(dtos.stream().map(dto -> getService().create(dto.withId(null))).toList());
    }

    @PutMapping("/{id}")
    public ResponseEntity<D> update(@PathVariable ID id, @RequestBody D dto) { return ResponseEntity.ok(getService().update(dto.withId(id))); }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable ID id) { getService().delete(id); return ResponseEntity.noContent().build(); }

    @PostMapping("/{id}/restore")
    public ResponseEntity<D> restore(@PathVariable ID id) { return ResponseEntity.ok(getService().restore(id)); }
}
