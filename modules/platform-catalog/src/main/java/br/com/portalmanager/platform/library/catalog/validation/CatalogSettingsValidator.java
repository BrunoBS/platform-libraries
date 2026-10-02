package br.com.portalmanager.platform.library.catalog.validation;
import br.com.portalmanager.platform.library.catalog.dto.CatalogDTOContract;
import br.com.portalmanager.platform.library.messaging.validation.ValidationResult;
@FunctionalInterface
public interface CatalogSettingsValidator<D extends CatalogDTOContract<D>> {
    void validate(D dto, ValidationResult result);
    static <D extends CatalogDTOContract<D>> CatalogSettingsValidator<D> none() { return (dto, result) -> {}; }
}
