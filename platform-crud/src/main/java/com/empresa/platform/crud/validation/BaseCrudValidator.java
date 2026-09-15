package com.empresa.platform.crud.validation;

import com.empresa.platform.crud.dto.BaseCrudDTO;

public abstract class BaseCrudValidator<D extends BaseCrudDTO<?>> {

    public void validateForFind(D dto) {
        CrudValidationResult result = new CrudValidationResult();
        validateRequired(dto, result);
        if (!result.hasErrors()) {
            validateIdRequired(dto, result);
        }
        if (!result.hasErrors()) {
            validateFind(dto, result);
        }
        throwIfInvalid(result);
    }

    public void validateForCreate(D dto) {
        CrudValidationResult result = new CrudValidationResult();
        validateRequired(dto, result);
        if (!result.hasErrors()) {
            validateIdAbsent(dto, result);
        }
        if (!result.hasErrors()) {
            validateAttributes(dto, result);
            validateCreateIntegrity(dto, result);
            validateAdditionalCreate(dto, result);
        }
        throwIfInvalid(result);
    }

    public void validateForUpdate(D dto) {
        CrudValidationResult result = new CrudValidationResult();
        validateRequired(dto, result);
        if (!result.hasErrors()) {
            validateIdRequired(dto, result);
        }
        if (!result.hasErrors()) {
            validateAttributes(dto, result);
            validateUpdateIntegrity(dto, result);
            validateAdditionalUpdate(dto, result);
        }
        throwIfInvalid(result);
    }

    public void validateForDelete(D dto) {
        CrudValidationResult result = new CrudValidationResult();
        validateRequired(dto, result);
        if (!result.hasErrors()) {
            validateIdRequired(dto, result);
        }
        if (!result.hasErrors()) {
            validateDelete(dto, result);
        }
        throwIfInvalid(result);
    }

    protected void validateRequired(D dto, CrudValidationResult result) {
        if (dto == null) {
            result.addError(entityName(), requiredMessageKey());
        }
    }

    protected void validateIdRequired(D dto, CrudValidationResult result) {
        if (dto.id() == null) {
            result.addError("id", idRequiredMessageKey());
        }
    }

    protected void validateIdAbsent(D dto, CrudValidationResult result) {
        if (dto.id() != null) {
            result.addError("id", idMustBeAbsentMessageKey());
        }
    }

    protected void validateFind(D dto, CrudValidationResult result) {
    }

    protected void validateAttributes(D dto, CrudValidationResult result) {
    }

    protected void validateCreateIntegrity(D dto, CrudValidationResult result) {
    }

    protected void validateUpdateIntegrity(D dto, CrudValidationResult result) {
    }

    protected void validateDelete(D dto, CrudValidationResult result) {
    }

    protected void validateAdditionalCreate(D dto, CrudValidationResult result) {
    }

    protected void validateAdditionalUpdate(D dto, CrudValidationResult result) {
    }

    protected String requiredMessageKey() {
        return "validation.required";
    }

    protected String idRequiredMessageKey() {
        return "validation.id.required";
    }

    protected String idMustBeAbsentMessageKey() {
        return "validation.id.must-be-absent";
    }

    protected void throwIfInvalid(CrudValidationResult result) {
        if (result.hasErrors()) {
            throw validationException(result);
        }
    }

    /**
     * Default validation failure for consumers that do not need a custom
     * exception strategy. Applications may still override this method.
     */
    protected RuntimeException validationException(CrudValidationResult result) {
        return new CrudValidationException(result);
    }

    public abstract String entityName();
}
