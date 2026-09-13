package com.empresa.platform.crud.mapper;

public interface BaseCrudMapper<E, D> {

    E toEntity(D dto);

    D toDTO(E entity);

    void updateEntity(E entity, D dto);
}
