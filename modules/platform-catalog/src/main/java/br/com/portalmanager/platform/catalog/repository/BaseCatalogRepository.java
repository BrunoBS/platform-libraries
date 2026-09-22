package br.com.portalmanager.platform.catalog.repository;

import br.com.portalmanager.platform.catalog.model.BaseCatalogEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.repository.NoRepositoryBean;

import java.util.List;
import java.util.Optional;

@NoRepositoryBean
public interface BaseCatalogRepository<E extends BaseCatalogEntity>
        extends JpaRepository<E, String>, JpaSpecificationExecutor<E> {

    Optional<E> findByCodeAndActiveTrue(String code);

    Optional<E> findByCodeAndActiveFalse(String code);

    List<E> findByActive(boolean active);

    Optional<E> findFirstByOrderBySortOrderDesc();

    Optional<E> findFirstByCodeNotOrderBySortOrderDesc(String code);

    List<E> findByCodeInAndActiveTrue(List<String> codes);
}
