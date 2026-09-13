package com.empresa.platform.crud.validation;

import com.empresa.platform.messaging.exception.ValidationException;
import com.empresa.platform.messaging.message.PlatformMessageKeys;
import com.empresa.platform.messaging.validation.ValidationResult;

public abstract class BaseCrudValidator<D, ID> {

    public void validateForCreate(D dto) {
        ValidationResult result = new ValidationResult();
        validateRequired(dto, result);
        if (!result.hasErrors()) {
            validateAttributes(dto, result);
            validateCreateIntegrity(dto, result);
            validateAdditionalCreate(dto, result);
        }
        throwIfInvalid(result);
    }

    public void validateForUpdate(ID id, D dto) {
        ValidationResult result = new ValidationResult();
        validateRequired(dto, result);
        if (!result.hasErrors()) {
            validateAttributes(dto, result);
            validateUpdateIntegrity(id, dto, result);
            validateAdditionalUpdate(id, dto, result);
        }
        throwIfInvalid(result);
    }

    public void validateForDelete(ID id) {
        ValidationResult result = new ValidationResult();
        validateDelete(id, result);
        throwIfInvalid(result);
    }

    protected void validateRequired(D dto, ValidationResult result) {
        if (dto == null) {
            result.addError(entityName(), requiredMessageKey());
        }
    }

    protected void validateAttributes(D dto, ValidationResult result) {
    }

    protected void validateCreateIntegrity(D dto, ValidationResult result) {
    }

    protected void validateUpdateIntegrity(ID id, D dto, ValidationResult result) {
    }

    protected void validateDelete(ID id, ValidationResult result) {
    }

    protected void validateAdditionalCreate(D dto, ValidationResult result) {
    }

    protected void validateAdditionalUpdate(ID id, D dto, ValidationResult result) {
    }

    protected String requiredMessageKey() {
        return "validation.required";
    }

    protected void throwIfInvalid(ValidationResult result) {
        if (result.hasErrors()) {
            throw new ValidationException(PlatformMessageKeys.VALIDATION_FAILED, result.getDetails());
        }
    }

    public abstract String entityName();
}
