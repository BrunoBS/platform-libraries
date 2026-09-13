package com.empresa.platform.catalog.validation;

import com.empresa.platform.catalog.dto.BaseCatalogDTO;
import com.empresa.platform.catalog.message.CatalogMessageKeys;
import com.empresa.platform.crud.validation.BaseCrudValidator;
import com.empresa.platform.messaging.validation.ValidationResult;

public abstract class BaseValidator<D extends BaseCatalogDTO<D>>
        extends BaseCrudValidator<D, Long> {

    @Override
    protected String requiredMessageKey() {
        return CatalogMessageKeys.REQUIRED;
    }

    @Override
    protected void validateCreateIntegrity(D dto, ValidationResult result) {
        validateIntegrity(dto, result);
    }

    @Override
    protected void validateUpdateIntegrity(Long id, D dto, ValidationResult result) {
        validateIntegrity(dto, result);
    }

    @Override
    protected void validateAdditionalCreate(D dto, ValidationResult result) {
        validateAdditionalFields(dto, result);
    }

    @Override
    protected void validateAdditionalUpdate(Long id, D dto, ValidationResult result) {
        validateAdditionalFields(dto, result);
    }

    protected void validateAdditionalFields(D dto, ValidationResult result) {
    }

    protected abstract void validateIntegrity(D dto, ValidationResult result);

    @Override
    protected abstract void validateAttributes(D dto, ValidationResult result);
}
