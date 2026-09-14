package com.empresa.platform.crud.validation;

public abstract class BaseCrudValidator<D, ID> {

    public void validateForFind(D dto) {
        CrudValidationResult result = new CrudValidationResult();
        validateRequired(dto, result);
        if (!result.hasErrors()) {
            validateFind(dto, result);
        }
        throwIfInvalid(result);
    }

    public void validateForCreate(D dto) {
        CrudValidationResult result = new CrudValidationResult();
        validateRequired(dto, result);
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
            validateDelete(dto, result);
        }
        throwIfInvalid(result);
    }

    protected void validateRequired(D dto, CrudValidationResult result) {
        if (dto == null) {
            result.addError(entityName(), requiredMessageKey());
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

    protected void throwIfInvalid(CrudValidationResult result) {
        if (result.hasErrors()) {
            throw validationException(result);
        }
    }

    /**
     * The CRUD module detects validation failures but does not own the
     * application's exception, HTTP or messaging semantics.
     */
    protected abstract RuntimeException validationException(CrudValidationResult result);

    public abstract String entityName();
}
