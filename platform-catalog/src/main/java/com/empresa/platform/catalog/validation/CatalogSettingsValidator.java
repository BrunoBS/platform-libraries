package com.empresa.platform.catalog.validation;

import com.empresa.platform.catalog.dto.BaseCatalogDTO;
import com.empresa.platform.crud.validation.CrudValidationResult;

@FunctionalInterface
public interface CatalogSettingsValidator<D extends BaseCatalogDTO<D>> {

    void validate(D dto, CrudValidationResult result);

    static <D extends BaseCatalogDTO<D>> CatalogSettingsValidator<D> none() {
        return (dto, result) -> { };
    }
}
