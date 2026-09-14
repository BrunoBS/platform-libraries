package com.empresa.platform.catalog.validation;

import com.empresa.platform.catalog.dto.BaseCatalogDTO;
import com.empresa.platform.catalog.message.CatalogMessageKeys;
import com.empresa.platform.crud.validation.BaseCrudValidator;
import com.empresa.platform.crud.validation.CrudValidationResult;
import com.empresa.platform.messaging.exception.ValidationException;
import com.empresa.platform.messaging.message.PlatformMessageKeys;
import com.empresa.platform.messaging.model.ValidationDetail;

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
        return new ValidationException(
                PlatformMessageKeys.VALIDATION_FAILED,
                result.getDetails().stream()
                        .map(detail -> new ValidationDetail(
                                detail.field(),
                                detail.messageKey(),
                                detail.parameters()
                        ))
                        .toList()
        );
    }

    protected void validateAdditionalFields(D dto, CrudValidationResult result) {
    }

    protected abstract void validateIntegrity(D dto, CrudValidationResult result);

    @Override
    protected abstract void validateAttributes(D dto, CrudValidationResult result);
}
