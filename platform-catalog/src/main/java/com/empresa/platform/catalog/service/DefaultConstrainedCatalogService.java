package com.empresa.platform.catalog.service;

import com.empresa.platform.catalog.dto.DefaultCatalogDTO;
import com.empresa.platform.catalog.mapper.DefaultCatalogMapper;
import com.empresa.platform.catalog.model.BaseCatalogEntity;
import com.empresa.platform.catalog.model.CatalogEnum;
import com.empresa.platform.catalog.repository.BaseCatalogRepository;
import com.empresa.platform.catalog.validation.CatalogSettingsValidator;
import com.empresa.platform.catalog.validation.DefaultEnumCatalogValidator;
import tools.jackson.databind.ObjectMapper;

/**
 * Convenience service for standard MANAGED_CONSTRAINED catalogs.
 */
public abstract class DefaultConstrainedCatalogService<
        E extends BaseCatalogEntity,
        C extends Enum<C> & CatalogEnum<C>>
        extends BaseCatalogService<E, DefaultCatalogDTO> {

    protected DefaultConstrainedCatalogService(
            BaseCatalogRepository<E> repository,
            ObjectMapper objectMapper,
            Class<E> entityClass,
            Class<C> enumClass) {
        this(repository, objectMapper, entityClass, enumClass, CatalogSettingsValidator.none());
    }

    protected DefaultConstrainedCatalogService(
            BaseCatalogRepository<E> repository,
            ObjectMapper objectMapper,
            Class<E> entityClass,
            Class<C> enumClass,
            CatalogSettingsValidator<DefaultCatalogDTO> settingsValidator) {
        super(
                repository,
                new DefaultCatalogMapper<>(entityClass, objectMapper),
                new DefaultEnumCatalogValidator<>(
                        repository,
                        enumClass,
                        entityClass.getSimpleName(),
                        settingsValidator
                )
        );
    }
}
