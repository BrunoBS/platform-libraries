package br.com.portalmanager.platform.library.catalog.repository;

import br.com.portalmanager.platform.library.catalog.model.CatalogEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.repository.NoRepositoryBean;

import java.util.List;
import java.util.Optional;

@NoRepositoryBean
public interface CatalogRepository<E extends CatalogEntity>
        extends JpaRepository<E, String>, JpaSpecificationExecutor<E> {

    Optional<E> findByCodeAndActiveTrue(String code);

    boolean existsByCodeAndActiveTrue(String code);

    Optional<E> findByCodeAndActiveFalse(String code);

    Optional<E> findFirstByOrderBySortOrderDesc();

    Optional<E> findFirstByCodeNotOrderBySortOrderDesc(String code);

    List<E> findByCodeInAndActiveTrue(List<String> codes);
}
