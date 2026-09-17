package com.empresa.platform.crud.normalization;

@FunctionalInterface
public interface CrudNormalizer<D> {

    D normalize(D dto);
}
