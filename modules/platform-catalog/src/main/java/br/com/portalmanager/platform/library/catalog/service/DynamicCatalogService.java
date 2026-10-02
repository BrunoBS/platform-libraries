package br.com.portalmanager.platform.library.catalog.service;

import br.com.portalmanager.platform.library.catalog.dto.CatalogDTO;
import br.com.portalmanager.platform.library.catalog.mapper.CatalogMapper;
import br.com.portalmanager.platform.library.catalog.model.CatalogEntity;
import br.com.portalmanager.platform.library.catalog.repository.CatalogRepository;
import br.com.portalmanager.platform.library.catalog.validation.AbstractCatalogValidator;
import br.com.portalmanager.platform.library.catalog.validation.CatalogSettingsValidator;
import tools.jackson.databind.ObjectMapper;
import br.com.portalmanager.platform.library.messaging.validation.ValidationResult;

/**
 * Simple path for fully dynamic catalogs: the database is the source of truth
 * for allowed catalog codes.
 */
public abstract non-sealed class DynamicCatalogService<E extends CatalogEntity>
        extends AbstractCatalogService<E, CatalogDTO> {

    protected DynamicCatalogService(
            CatalogRepository<E> repository,
            ObjectMapper objectMapper,
            Class<E> entityClass) {
        this(repository, objectMapper, entityClass, CatalogSettingsValidator.none());
    }

    protected DynamicCatalogService(
            CatalogRepository<E> repository,
            ObjectMapper objectMapper,
            Class<E> entityClass,
            CatalogSettingsValidator<CatalogDTO> settingsValidator) {
        super(
                repository,
                new CatalogMapper<>(entityClass, objectMapper),
                validator(repository, entityClass, settingsValidator)
        );
    }

    private static <E extends CatalogEntity> AbstractCatalogValidator<CatalogDTO> validator(
            CatalogRepository<E> repository,
            Class<E> entityClass,
            CatalogSettingsValidator<CatalogDTO> settingsValidator) {
        CatalogSettingsValidator<CatalogDTO> resolved = settingsValidator == null
                ? CatalogSettingsValidator.none()
                : settingsValidator;

        return new AbstractCatalogValidator<>(repository) {
            @Override
            protected void validateSettings(CatalogDTO dto, ValidationResult result) {
                resolved.validate(dto, result);
            }

            @Override
            public String entityName() {
                return entityClass.getSimpleName();
            }
        };
    }
}
