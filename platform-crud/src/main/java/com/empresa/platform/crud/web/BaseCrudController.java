package com.empresa.platform.crud.web;

import com.empresa.platform.crud.dto.BaseCrudDTO;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.ResponseStatus;

import java.util.List;

public abstract class BaseCrudController<
        E,
        D extends BaseCrudDTO<ID, D>,
        ID>
        extends CrudControllerSupport<E, D, ID> {

    @GetMapping
    public List<D> findAll() {
        return service().findAll();
    }

    @GetMapping("/{id}")
    public D findById(@PathVariable ID id) {
        return findByIdInternal(id);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public D create(@RequestBody D dto) {
        return createInternal(dto);
    }

    @PutMapping("/{id}")
    public D update(@PathVariable ID id, @RequestBody D dto) {
        return updateInternal(id, dto);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable ID id) {
        deleteInternal(id);
    }
}
