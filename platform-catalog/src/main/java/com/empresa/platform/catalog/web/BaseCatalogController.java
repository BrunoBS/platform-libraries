package com.empresa.platform.catalog.web;

import com.empresa.platform.catalog.dto.BaseCatalogDTO;
import com.empresa.platform.catalog.model.BaseCatalogEntity;
import com.empresa.platform.catalog.service.BaseCatalogService;
import com.empresa.platform.crud.web.BaseCrudController;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;

public abstract class BaseCatalogController<
        D extends BaseCatalogDTO<D>,
        E extends BaseCatalogEntity>
        extends BaseCrudController<E, D, Long> {

    protected abstract BaseCatalogService<E, D> getService();

    @Override
    protected final BaseCatalogService<E, D> service() {
        return getService();
    }

    @PostMapping("/{id}/restore")
    public D restore(@PathVariable Long id) {
        return getService().restore(id);
    }
}
