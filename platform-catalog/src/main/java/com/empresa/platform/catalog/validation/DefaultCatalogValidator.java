package com.empresa.platform.catalog.validation;

import com.empresa.platform.catalog.dto.DefaultCatalogDTO;
import com.empresa.platform.catalog.repository.BaseCatalogRepository;
import com.empresa.platform.crud.validation.CrudValidationResult;

/**
 * Default validator for standard MANAGED catalogs.
 */
public class DefaultCatalogValidator extends BaseCatalogValidator<DefaultCatalogDTO> {

    private final String entityName;
    private final CatalogSettingsValidator<DefaultCatalogDTO> settingsValidator;

    public DefaultCatalogValidator(
            BaseCatalogRepository<?> repository,
            String entityName) {
        this(repository, entityName, CatalogSettingsValidator.none());
    }

    public DefaultCatalogValidator(
            BaseCatalogRepository<?> repository,
            String entityName,
            CatalogSettingsValidator<DefaultCatalogDTO> settingsValidator) {
        super(repository);
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
