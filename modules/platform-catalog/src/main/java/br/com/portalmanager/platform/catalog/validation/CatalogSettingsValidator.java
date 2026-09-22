package br.com.portalmanager.platform.catalog.validation;
import br.com.portalmanager.platform.catalog.dto.CatalogDTOContract;
@FunctionalInterface
public interface CatalogSettingsValidator<D extends CatalogDTOContract<D>> {
    void validate(D dto, CatalogValidationResult result);
    static <D extends CatalogDTOContract<D>> CatalogSettingsValidator<D> none() { return (dto, result) -> {}; }
}
