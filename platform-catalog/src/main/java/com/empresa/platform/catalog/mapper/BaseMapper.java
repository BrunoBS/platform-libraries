package com.empresa.platform.catalog.mapper;

import com.empresa.platform.crud.mapper.BaseCrudMapper;

/**
 * Backward-compatible catalog mapper contract.
 *
 * @param <D> catalog DTO type
 * @param <E> catalog entity type
 */
public interface BaseMapper<D, E> extends BaseCrudMapper<E, D> {
}
