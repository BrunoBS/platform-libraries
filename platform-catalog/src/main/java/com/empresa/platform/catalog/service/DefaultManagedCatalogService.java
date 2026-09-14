package com.empresa.platform.catalog.service;

import com.empresa.platform.catalog.dto.DefaultCatalogDTO;
import com.empresa.platform.catalog.mapper.DefaultCatalogMapper;
import com.empresa.platform.catalog.model.BaseCatalogEntity;
import com.empresa.platform.catalog.repository.BaseCatalogRepository;
import com.empresa.platform.catalog.validation.CatalogSettingsValidator;
import com.empresa.platform.catalog.validation.DefaultCatalogValidator;
import tools.jackson.databind.ObjectMapper;

/**
 * Convenience service for standard MANAGED catalogs.
 *
 * Consumers only need a dedicated DTO, mapper or validator when the catalog
 * has additional fields or rules beyond the standard contract.
 */
public abstract class DefaultManagedCatalogService<E extends BaseCatalogEntity>
        extends BaseCatalogService<E, DefaultCatalogDTO> {

    protected DefaultManagedCatalogService(
            BaseCatalogRepository<E> repository,
            ObjectMapper objectMapper,
            Class<E> entityClass) {
        this(repository, objectMapper, entityClass, CatalogSettingsValidator.none());
    }

    protected DefaultManagedCatalogService(
            BaseCatalogRepository<E> repository,
            ObjectMapper objectMapper,
            Class<E> entityClass,
            CatalogSettingsValidator<DefaultCatalogDTO> settingsValidator) {
        super(
                repository,
                new DefaultCatalogMapper<>(entityClass, objectMapper),
                new DefaultCatalogValidator(repository, entityClass.getSimpleName(), settingsValidator)
        );
    }
}
