package br.com.portalmanager.core.catalog.validation;
import br.com.portalmanager.core.catalog.dto.BaseCatalogDTO;
@FunctionalInterface
public interface CatalogSettingsValidator<D extends BaseCatalogDTO<D>> {
    void validate(D dto, CatalogValidationResult result);
    static <D extends BaseCatalogDTO<D>> CatalogSettingsValidator<D> none() { return (dto, result) -> {}; }
}
