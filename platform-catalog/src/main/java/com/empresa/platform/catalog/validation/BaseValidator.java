package com.empresa.platform.catalog.validation;

import com.empresa.platform.catalog.dto.BaseCatalogDTO;
import com.empresa.platform.catalog.exception.CatalogValidationException;
import com.empresa.platform.catalog.message.CatalogMessageKeys;
import com.empresa.platform.crud.validation.BaseCrudValidator;
import com.empresa.platform.crud.validation.CrudValidationResult;

import java.util.Map;

public abstract class BaseValidator<D extends BaseCatalogDTO<D>>
        extends BaseCrudValidator<D, Long> {

    @Override
    protected String requiredMessageKey() {
        return CatalogMessageKeys.REQUIRED;
    }

    @Override
    protected void validateCreateIntegrity(D dto, CrudValidationResult result) {
        validateIntegrity(dto, result);
    }

    @Override
    protected void validateUpdateIntegrity(Long id, D dto, CrudValidationResult result) {
        validateIntegrity(dto, result);
    }

    @Override
    protected void validateAdditionalCreate(D dto, CrudValidationResult result) {
        validateAdditionalFields(dto, result);
    }

    @Override
    protected void validateAdditionalUpdate(Long id, D dto, CrudValidationResult result) {
        validateAdditionalFields(dto, result);
    }

    @Override
    protected RuntimeException validationException(CrudValidationResult result) {
        return new CatalogValidationException(
                CatalogMessageKeys.VALIDATION_FAILED,
                result.getDetails(),
                Map.of("0", entityName())
        );
    }

    protected void validateAdditionalFields(D dto, CrudValidationResult result) {
    }

    protected abstract void validateIntegrity(D dto, CrudValidationResult result);

    @Override
    protected abstract void validateAttributes(D dto, CrudValidationResult result);
}
