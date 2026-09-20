package com.empresa.platform.catalog.validation;

import com.empresa.platform.catalog.dto.BaseCatalogDTO;
import com.empresa.platform.catalog.message.CatalogMessageKeys;
import com.empresa.platform.catalog.model.BaseCatalogEntity;
import com.empresa.platform.catalog.repository.BaseCatalogRepository;
import com.empresa.platform.messaging.exception.ValidationException;
import com.empresa.platform.messaging.message.PlatformMessageKeys;
import java.util.Map;
import java.util.Objects;

public abstract class BaseCatalogValidator<D extends BaseCatalogDTO<D>> {
    protected final BaseCatalogRepository<?> repository;
    protected BaseCatalogValidator(BaseCatalogRepository<?> repository) { this.repository = repository; }

    public void validateForCreate(D dto) {
        CatalogValidationResult result = new CatalogValidationResult();
        validateRequired(dto, result);
        if (!result.hasErrors() && dto.id() != null) result.addError("id", PlatformMessageKeys.VALIDATION_FAILED);
        if (!result.hasErrors()) { validateAttributes(dto, result); validateIntegrity(dto, result); validateAdditionalFields(dto, result); }
        throwIfInvalid(result);
    }
    public void validateForUpdate(D dto) {
        CatalogValidationResult result = new CatalogValidationResult();
        validateRequired(dto, result);
        if (!result.hasErrors() && dto.id() == null) result.addError("id", CatalogMessageKeys.REQUIRED, Map.of("0", entityName()));
        if (!result.hasErrors()) { validateAttributes(dto, result); validateNameMutability(dto, result); validateIntegrity(dto, result); validateAdditionalFields(dto, result); }
        throwIfInvalid(result);
    }
    public void validateForDelete(D dto) {
        CatalogValidationResult result = new CatalogValidationResult();
        validateRequired(dto, result);
        if (!result.hasErrors() && dto.id() == null) result.addError("id", CatalogMessageKeys.REQUIRED, Map.of("0", entityName()));
        if (!result.hasErrors()) validateDelete(dto, result);
        throwIfInvalid(result);
    }
    protected void validateRequired(D dto, CatalogValidationResult result) {
        if (dto == null) result.addError(entityName(), CatalogMessageKeys.REQUIRED);
    }
    protected void validateAttributes(D dto, CatalogValidationResult result) {
        if (dto.name() == null || dto.name().isBlank()) result.addError("name", CatalogMessageKeys.NAME_REQUIRED, Map.of("0", entityName()));
        if (dto.label() == null || dto.label().isBlank()) result.addError("label", CatalogMessageKeys.LABEL_REQUIRED, Map.of("0", entityName()));
        if (dto.description() == null || dto.description().isBlank()) result.addError("description", CatalogMessageKeys.DESCRIPTION_REQUIRED, Map.of("0", entityName()));
        else if (dto.description().length() < 3 || dto.description().length() > 250)
            result.addError("description", CatalogMessageKeys.DESCRIPTION_INVALID_LENGTH, Map.of("0", entityName(), "1", 3, "2", 250));
        validateSettings(dto, result);
        validateAdditionalCatalogFields(dto, result);
    }
    protected void validateIntegrity(D dto, CatalogValidationResult result) { validateUniqueness(dto, result); validateAdditionalIntegrity(dto, result); }
    protected void validateUniqueness(D dto, CatalogValidationResult result) {
        long id = dto.id() == null ? 0L : dto.id();
        if (repository.existsByNameAndIdNot(dto.name(), id))
            result.addError("name", CatalogMessageKeys.NAME_DUPLICATE, Map.of("0", entityName(), "1", dto.name()));
    }
    protected void validateNameMutability(D dto, CatalogValidationResult result) {
        if (isNameMutable() || dto.id() == null) return;
        repository.findById(dto.id()).map(BaseCatalogEntity.class::cast).ifPresent(entity -> {
            if (!Objects.equals(entity.getName(), dto.name()))
                result.addError("name", CatalogMessageKeys.NAME_IMMUTABLE, Map.of("0", entityName(), "1", entity.getName()));
        });
    }
    protected void throwIfInvalid(CatalogValidationResult result) {
        if (result.hasErrors()) throw new ValidationException(PlatformMessageKeys.VALIDATION_FAILED, result.getDetails());
    }
    protected boolean isNameMutable() { return false; }
    protected void validateDelete(D dto, CatalogValidationResult result) {}
    protected void validateSettings(D dto, CatalogValidationResult result) {}
    protected void validateAdditionalFields(D dto, CatalogValidationResult result) {}
    protected void validateAdditionalCatalogFields(D dto, CatalogValidationResult result) {}
    protected void validateAdditionalIntegrity(D dto, CatalogValidationResult result) {}
    public abstract String entityName();
}
