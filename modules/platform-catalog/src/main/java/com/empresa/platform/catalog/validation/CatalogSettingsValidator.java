package com.empresa.platform.catalog.validation;
import com.empresa.platform.catalog.dto.BaseCatalogDTO;
@FunctionalInterface
public interface CatalogSettingsValidator<D extends BaseCatalogDTO<D>> {
    void validate(D dto, CatalogValidationResult result);
    static <D extends BaseCatalogDTO<D>> CatalogSettingsValidator<D> none() { return (dto, result) -> {}; }
}
