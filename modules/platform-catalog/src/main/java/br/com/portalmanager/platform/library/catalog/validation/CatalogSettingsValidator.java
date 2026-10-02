package br.com.portalmanager.platform.library.catalog.validation;

import br.com.portalmanager.platform.library.catalog.dto.CatalogDTO;
import br.com.portalmanager.platform.library.messaging.validation.ValidationResult;

@FunctionalInterface
public interface CatalogSettingsValidator {
    void validate(CatalogDTO dto, ValidationResult result);

    static CatalogSettingsValidator none() {
        return (dto, result) -> {};
    }
}
