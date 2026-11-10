package br.com.portalmanager.platform.library.catalog.internal.validation;

import br.com.portalmanager.platform.library.catalog.api.contract.CatalogDTO;
import br.com.portalmanager.platform.library.messaging.api.validation.ValidationResult;
import br.com.portalmanager.platform.library.schemavalidation.api.contract.validation.SchemaValidator;

@FunctionalInterface
public interface CatalogSettingsValidator {

    void validate(CatalogDTO dto, ValidationResult result);

    static CatalogSettingsValidator none() {
        return (dto, result) -> {};
    }

    static CatalogSettingsValidator schema(
            String resourceCode,
            SchemaValidator schemaValidator
    ) {
        return new SchemaCatalogSettingsValidator(resourceCode, schemaValidator);
    }
}
