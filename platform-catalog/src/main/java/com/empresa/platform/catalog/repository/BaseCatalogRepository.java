package com.empresa.platform.catalog.repository;

import com.empresa.platform.catalog.model.BaseCatalogEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.repository.NoRepositoryBean;

import java.util.List;
import java.util.Optional;

@NoRepositoryBean
public interface BaseCatalogRepository<E extends BaseCatalogEntity, ID> extends JpaRepository<E, ID> {
    Optional<E> findByNameAndActiveTrue(String name);
    Optional<E> findByIdAndActiveTrue(ID id);
    Optional<E> findByIdAndActiveFalse(ID id);
    List<E> findByActive(boolean active);
    boolean existsByNameAndIdNot(String name, ID id);
    Optional<E> findFirstByOrderBySortOrderDesc();
    Optional<E> findFirstByIdNotOrderBySortOrderDesc(ID id);
    List<E> findByNameInAndActiveTrue(List<String> names);
}
