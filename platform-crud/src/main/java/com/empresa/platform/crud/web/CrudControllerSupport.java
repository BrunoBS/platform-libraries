package com.empresa.platform.crud.web;

import com.empresa.platform.crud.dto.BaseCrudDTO;
import com.empresa.platform.crud.service.BaseCrudService;

/**
 * Reusable controller support without declaring any HTTP contract.
 *
 * Concrete web abstractions may reuse the CRUD operations while keeping their
 * own endpoint shapes, request payloads and response semantics.
 */
public abstract class CrudControllerSupport<
        E,
        D extends BaseCrudDTO<ID, D>,
        ID> {

    protected abstract BaseCrudService<E, D, ID> service();

    protected D findByIdInternal(ID id) {
        return service().findById(id);
    }

    protected D createInternal(D dto) {
        return service().create(dto);
    }

    protected D updateInternal(ID id, D dto) {
        return service().update(id, dto);
    }

    protected void deleteInternal(ID id) {
        service().delete(id);
    }
}
