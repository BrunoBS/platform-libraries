package com.empresa.platform.catalog.validation;

import com.empresa.platform.catalog.dto.DefaultCatalogDTO;
import com.empresa.platform.catalog.model.CatalogEnum;
import com.empresa.platform.catalog.repository.BaseCatalogRepository;
import com.empresa.platform.crud.validation.CrudValidationResult;

/**
 * Default validator for standard MANAGED_CONSTRAINED catalogs.
 */
public class DefaultEnumCatalogValidator<E extends Enum<E> & CatalogEnum<E>>
        extends EnumCatalogValidator<E, DefaultCatalogDTO> {

    private final String entityName;
    private final CatalogSettingsValidator<DefaultCatalogDTO> settingsValidator;

    public DefaultEnumCatalogValidator(
            BaseCatalogRepository<?> repository,
            Class<E> enumClass,
            String entityName) {
        this(repository, enumClass, entityName, CatalogSettingsValidator.none());
    }

    public DefaultEnumCatalogValidator(
            BaseCatalogRepository<?> repository,
            Class<E> enumClass,
            String entityName,
            CatalogSettingsValidator<DefaultCatalogDTO> settingsValidator) {
        super(repository, enumClass);
        this.entityName = entityName;
        this.settingsValidator = settingsValidator == null
                ? CatalogSettingsValidator.none()
                : settingsValidator;
    }

    @Override
    protected void validateSettings(DefaultCatalogDTO dto, CrudValidationResult result) {
        settingsValidator.validate(dto, result);
    }

    @Override
    public String entityName() {
        return entityName;
    }
}
