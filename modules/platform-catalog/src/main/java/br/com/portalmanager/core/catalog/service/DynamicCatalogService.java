package br.com.portalmanager.core.catalog.service;

import br.com.portalmanager.core.catalog.dto.CatalogDTO;
import br.com.portalmanager.core.catalog.mapper.CatalogMapper;
import br.com.portalmanager.core.catalog.model.BaseCatalogEntity;
import br.com.portalmanager.core.catalog.repository.BaseCatalogRepository;
import br.com.portalmanager.core.catalog.validation.BaseCatalogValidator;
import br.com.portalmanager.core.catalog.validation.CatalogSettingsValidator;
import br.com.portalmanager.core.catalog.validation.CatalogValidationResult;
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
            protected void validateSettings(CatalogDTO dto, CatalogValidationResult result) {
                resolved.validate(dto, result);
            }

            @Override
            public String entityName() {
                return entityClass.getSimpleName();
            }
        };
    }
}
