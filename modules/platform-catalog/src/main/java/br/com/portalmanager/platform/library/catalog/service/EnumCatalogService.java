package br.com.portalmanager.platform.library.catalog.service;

import br.com.portalmanager.platform.library.catalog.dto.CatalogDTO;
import br.com.portalmanager.platform.library.catalog.mapper.CatalogMapper;
import br.com.portalmanager.platform.library.catalog.model.CatalogEntity;
import br.com.portalmanager.platform.library.catalog.model.CatalogEnum;
import br.com.portalmanager.platform.library.catalog.repository.CatalogRepository;
import br.com.portalmanager.platform.library.catalog.validation.CatalogSettingsValidator;
import br.com.portalmanager.platform.library.catalog.validation.EnumCatalogValidator;
import br.com.portalmanager.platform.library.catalog.validation.CatalogValidationResult;
import tools.jackson.databind.ObjectMapper;

/**
 * Simple path for catalogs whose allowed codes are defined by a Java enum.
 * The database still owns label, description, sort order, active state and settings.
 */
public abstract non-sealed class EnumCatalogService<
        E extends CatalogEntity,
        C extends Enum<C> & CatalogEnum<C>>
        extends AbstractCatalogService<E, CatalogDTO> {

    protected EnumCatalogService(
            CatalogRepository<E> repository,
            ObjectMapper objectMapper,
            Class<E> entityClass,
            Class<C> enumClass) {
        this(repository, objectMapper, entityClass, enumClass, CatalogSettingsValidator.none());
    }

    protected EnumCatalogService(
            CatalogRepository<E> repository,
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

    private static <E extends CatalogEntity, C extends Enum<C> & CatalogEnum<C>>
    EnumCatalogValidator<C, CatalogDTO> validator(
            CatalogRepository<E> repository,
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
