package com.empresa.platform.crud.validation;

public abstract class BaseCrudValidator<D, ID> {

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

    public void validateForUpdate(ID id, D dto) {
        CrudValidationResult result = new CrudValidationResult();
        validateRequired(dto, result);
        if (!result.hasErrors()) {
            validateAttributes(dto, result);
            validateUpdateIntegrity(id, dto, result);
            validateAdditionalUpdate(id, dto, result);
        }
        throwIfInvalid(result);
    }

    public void validateForDelete(ID id) {
        CrudValidationResult result = new CrudValidationResult();
        validateDelete(id, result);
        throwIfInvalid(result);
    }

    protected void validateRequired(D dto, CrudValidationResult result) {
        if (dto == null) {
            result.addError(entityName(), requiredMessageKey());
        }
    }

    protected void validateAttributes(D dto, CrudValidationResult result) {
    }

    protected void validateCreateIntegrity(D dto, CrudValidationResult result) {
    }

    protected void validateUpdateIntegrity(ID id, D dto, CrudValidationResult result) {
    }

    protected void validateDelete(ID id, CrudValidationResult result) {
    }

    protected void validateAdditionalCreate(D dto, CrudValidationResult result) {
    }

    protected void validateAdditionalUpdate(ID id, D dto, CrudValidationResult result) {
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
