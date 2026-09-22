package br.com.portalmanager.platform.catalog.service;

import br.com.portalmanager.platform.catalog.dto.CatalogDTO;
import br.com.portalmanager.platform.catalog.mapper.CatalogMapper;
import br.com.portalmanager.platform.catalog.model.BaseCatalogEntity;
import br.com.portalmanager.platform.catalog.model.CatalogEnum;
import br.com.portalmanager.platform.catalog.repository.BaseCatalogRepository;
import br.com.portalmanager.platform.catalog.validation.CatalogSettingsValidator;
import br.com.portalmanager.platform.catalog.validation.EnumCatalogValidator;
import br.com.portalmanager.platform.catalog.validation.CatalogValidationResult;
import tools.jackson.databind.ObjectMapper;

/**
 * Simple path for catalogs whose allowed names are defined by a Java enum.
 * The database still owns label, description, sort order, active state and settings.
 */
public abstract non-sealed class EnumCatalogService<
        E extends BaseCatalogEntity,
        C extends Enum<C> & CatalogEnum<C>>
        extends BaseCatalogService<E, CatalogDTO> {

    protected EnumCatalogService(
            BaseCatalogRepository<E> repository,
            ObjectMapper objectMapper,
            Class<E> entityClass,
            Class<C> enumClass) {
        this(repository, objectMapper, entityClass, enumClass, CatalogSettingsValidator.none());
    }

    protected EnumCatalogService(
            BaseCatalogRepository<E> repository,
            ObjectMapper objectMapper,
            Class<E> entityClass,
            Class<C> enumClass,
            CatalogSettingsValidator<CatalogDTO> settingsValidator) {
        super(
                repository,
                new CatalogMapper<>(entityClass, objectMapper),
                validator(repository, entityClass, enumClass, settingsValidator)
        );
    }

    private static <E extends BaseCatalogEntity, C extends Enum<C> & CatalogEnum<C>>
    EnumCatalogValidator<C, CatalogDTO> validator(
            BaseCatalogRepository<E> repository,
            Class<E> entityClass,
            Class<C> enumClass,
            CatalogSettingsValidator<CatalogDTO> settingsValidator) {
        CatalogSettingsValidator<CatalogDTO> resolved = settingsValidator == null
                ? CatalogSettingsValidator.none()
                : settingsValidator;

        return new EnumCatalogValidator<>(repository, enumClass) {
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
