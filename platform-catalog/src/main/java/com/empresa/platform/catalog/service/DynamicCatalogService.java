package com.empresa.platform.catalog.service;

import com.empresa.platform.catalog.dto.CatalogDTO;
import com.empresa.platform.catalog.mapper.CatalogMapper;
import com.empresa.platform.catalog.model.BaseCatalogEntity;
import com.empresa.platform.catalog.repository.BaseCatalogRepository;
import com.empresa.platform.catalog.validation.BaseCatalogValidator;
import com.empresa.platform.catalog.validation.CatalogSettingsValidator;
import com.empresa.platform.crud.validation.CrudValidationResult;
import tools.jackson.databind.ObjectMapper;

/**
 * Simple path for fully dynamic catalogs: the database is the source of truth
 * for allowed catalog names.
 */
public abstract class DynamicCatalogService<E extends BaseCatalogEntity>
        extends BaseCatalogService<E, CatalogDTO> {

    protected DynamicCatalogService(
            BaseCatalogRepository<E> repository,
            ObjectMapper objectMapper,
            Class<E> entityClass) {
        this(repository, objectMapper, entityClass, CatalogSettingsValidator.none());
    }

    protected DynamicCatalogService(
            BaseCatalogRepository<E> repository,
            ObjectMapper objectMapper,
            Class<E> entityClass,
            CatalogSettingsValidator<CatalogDTO> settingsValidator) {
        super(
                repository,
                new CatalogMapper<>(entityClass, objectMapper),
                validator(repository, entityClass, settingsValidator)
        );
    }

    private static <E extends BaseCatalogEntity> BaseCatalogValidator<CatalogDTO> validator(
            BaseCatalogRepository<E> repository,
            Class<E> entityClass,
            CatalogSettingsValidator<CatalogDTO> settingsValidator) {
        CatalogSettingsValidator<CatalogDTO> resolved = settingsValidator == null
                ? CatalogSettingsValidator.none()
                : settingsValidator;

        return new BaseCatalogValidator<>(repository) {
            @Override
            protected void validateSettings(CatalogDTO dto, CrudValidationResult result) {
                resolved.validate(dto, result);
            }

            @Override
            public String entityName() {
                return entityClass.getSimpleName();
            }
        };
    }
}
