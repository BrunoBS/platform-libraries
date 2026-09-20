package br.com.portalmanager.core.catalog.repository;

import br.com.portalmanager.core.catalog.model.BaseCatalogEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.repository.NoRepositoryBean;
import java.util.List;
import java.util.Optional;

@NoRepositoryBean
public interface BaseCatalogRepository<E extends BaseCatalogEntity>
        extends JpaRepository<E, Long>, JpaSpecificationExecutor<E> {
    Optional<E> findByNameAndActiveTrue(String name);
    Optional<E> findByIdAndActiveTrue(Long id);
    Optional<E> findByIdAndActiveFalse(Long id);
    List<E> findByActive(boolean active);
    boolean existsByNameAndIdNot(String name, Long id);
    Optional<E> findFirstByOrderBySortOrderDesc();
    Optional<E> findFirstByIdNotOrderBySortOrderDesc(Long id);
    List<E> findByNameInAndActiveTrue(List<String> names);
}
