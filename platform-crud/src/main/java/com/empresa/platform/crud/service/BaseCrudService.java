package com.empresa.platform.crud.service;

import com.empresa.platform.crud.dto.BaseCrudDTO;
import com.empresa.platform.crud.mapper.BaseCrudMapper;
import com.empresa.platform.crud.repository.BaseCrudRepository;
import com.empresa.platform.crud.validation.BaseCrudValidator;
import com.empresa.platform.crud.version.OptimisticLockSupport;
import com.empresa.platform.crud.version.OptimisticLockable;
import org.springframework.transaction.annotation.Transactional;

public abstract class BaseCrudService<
        E,
        D extends BaseCrudDTO<ID>,
        ID,
        R extends BaseCrudRepository<E, ID>> {

    private final R repository;
    private final BaseCrudMapper<E, D> mapper;
    private final BaseCrudValidator<D> validator;

    protected BaseCrudService(
            R repository,
            BaseCrudMapper<E, D> mapper,
            BaseCrudValidator<D> validator) {
        this.repository = repository;
        this.mapper = mapper;
        this.validator = validator;
    }

    @Transactional(readOnly = true)
    public D findById(D dto) {
        validator.validateForFind(dto);
        return mapper.toDTO(getEntity(dto));
    }

    @Transactional
    public D create(D dto) {
        validator.validateForCreate(dto);
        OptimisticLockSupport.validateCreate(dto);
        E entity = mapper.toEntity(dto);
        beforeCreate(entity, dto);
        E saved = repository.save(entity);
        afterCreate(saved, dto);
        flushIfVersioned(saved);
        return mapper.toDTO(saved);
    }

    @Transactional
    public D update(D dto) {
        validator.validateForUpdate(dto);
        E entity = getEntity(dto);
        OptimisticLockSupport.validate(entity, dto);
        mapper.updateEntity(entity, dto);
        beforeUpdate(entity, dto);
        E saved = repository.save(entity);
        afterUpdate(saved, dto);
        flushIfVersioned(saved);
        return mapper.toDTO(saved);
    }

    @Transactional
    public void delete(D dto) {
        validator.validateForDelete(dto);
        E entity = getEntity(dto);
        beforeDelete(entity);
        deleteEntity(entity);
        afterDelete(entity);
    }

    protected E getEntity(D dto) {
        return repository.findById(dto.id())
                .orElseThrow(() -> notFoundException(dto.id()));
    }

    protected abstract RuntimeException notFoundException(ID id);

    protected void deleteEntity(E entity) {
        repository.delete(entity);
    }

    protected void beforeCreate(E entity, D dto) {
    }

    protected void afterCreate(E entity, D dto) {
    }

    protected void beforeUpdate(E entity, D dto) {
    }

    protected void afterUpdate(E entity, D dto) {
    }

    protected void beforeDelete(E entity) {
    }

    protected void afterDelete(E entity) {
    }

    protected R repository() {
        return repository;
    }

    protected BaseCrudMapper<E, D> mapper() {
        return mapper;
    }

    protected BaseCrudValidator<D> validator() {
        return validator;
    }

    private void flushIfVersioned(E entity) {
        if (entity instanceof OptimisticLockable) {
            repository.flush();
        }
    }
}
