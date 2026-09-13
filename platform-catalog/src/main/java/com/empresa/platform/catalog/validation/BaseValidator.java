package com.empresa.platform.catalog.validation;

import com.empresa.platform.catalog.dto.BaseTypeDTO;
import com.empresa.platform.catalog.message.CatalogMessageKeys;
import com.empresa.platform.messaging.exception.ValidationException;
import com.empresa.platform.messaging.message.PlatformMessageKeys;
import com.empresa.platform.messaging.validation.ValidationResult;

public abstract class BaseValidator<D extends BaseTypeDTO<D, ID>, ID> {

    public void validateForCreate(D dto) {
        validate(dto);
    }

    public void validateForUpdate(D dto) {
        validate(dto);
    }

    protected void validate(D dto) {
        ValidationResult result = new ValidationResult();
        if (dto == null) {
            result.addError(entityName(), CatalogMessageKeys.REQUIRED);
            throw new ValidationException(PlatformMessageKeys.VALIDATION_FAILED, result.getDetails());
        }

        validateAttributes(dto, result);
        validateIntegrity(dto, result);
        validateAdditionalFields(dto, result);

        if (result.hasErrors()) {
            throw new ValidationException(PlatformMessageKeys.VALIDATION_FAILED, result.getDetails());
        }
    }

    public void validateForDelete(ID id) {
    }

    protected void validateAdditionalFields(D dto, ValidationResult result) {
    }

    protected abstract void validateIntegrity(D dto, ValidationResult result);
    protected abstract void validateAttributes(D dto, ValidationResult result);
    public abstract String entityName();
}
