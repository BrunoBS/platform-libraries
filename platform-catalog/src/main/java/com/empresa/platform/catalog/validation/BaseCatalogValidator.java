package com.empresa.platform.catalog.validation;

import com.empresa.platform.catalog.dto.BaseCatalogDTO;
import com.empresa.platform.catalog.message.CatalogMessageKeys;
import com.empresa.platform.catalog.model.BaseCatalogEntity;
import com.empresa.platform.catalog.repository.BaseCatalogRepository;
import com.empresa.platform.crud.validation.BaseCrudValidator;
import com.empresa.platform.crud.validation.CrudValidationResult;
import com.empresa.platform.messaging.exception.ValidationException;
import com.empresa.platform.messaging.message.PlatformMessageKeys;
import com.empresa.platform.messaging.model.ValidationDetail;

import java.util.Map;
import java.util.Objects;

/**
 * Base validator for catalogs that need custom validation rules.
 *
 * Name uniqueness is provided as the default policy but may be overridden by
 * scoped catalogs, for example account + name or application + name.
 */
public abstract class BaseCatalogValidator<D extends BaseCatalogDTO<D>>
        extends BaseCrudValidator<D> {

    protected final BaseCatalogRepository<?> repository;

    protected BaseCatalogValidator(BaseCatalogRepository<?> repository) {
        this.repository = repository;
    }

    @Override
    protected String requiredMessageKey() {
        return CatalogMessageKeys.REQUIRED;
    }

    @Override
    protected void validateAttributes(D dto, CrudValidationResult result) {
        if (dto.name() == null || dto.name().isBlank()) {
            result.addError("name", CatalogMessageKeys.NAME_REQUIRED, Map.of("0", entityName()));
        }
        if (dto.label() == null || dto.label().isBlank()) {
            result.addError("label", CatalogMessageKeys.LABEL_REQUIRED, Map.of("0", entityName()));
        }
        if (dto.description() == null || dto.description().isBlank()) {
            result.addError("description", CatalogMessageKeys.DESCRIPTION_REQUIRED, Map.of("0", entityName()));
        } else if (dto.description().length() < 3 || dto.description().length() > 250) {
            result.addError("description", CatalogMessageKeys.DESCRIPTION_INVALID_LENGTH,
                    Map.of("0", entityName(), "1", 3, "2", 250));
        }
        validateSettings(dto, result);
        validateAdditionalCatalogFields(dto, result);
    }

    @Override
    protected void validateCreateIntegrity(D dto, CrudValidationResult result) {
        validateIntegrity(dto, result);
    }

    @Override
    protected void validateUpdateIntegrity(D dto, CrudValidationResult result) {
        validateNameMutability(dto, result);
        validateIntegrity(dto, result);
    }

    @Override
    protected void validateAdditionalCreate(D dto, CrudValidationResult result) {
        validateAdditionalFields(dto, result);
    }

    @Override
    protected void validateAdditionalUpdate(D dto, CrudValidationResult result) {
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
                                detail.parameters(),
                                detail.defaultMessage()
                        ))
                        .toList()
        );
    }

    protected void validateIntegrity(D dto, CrudValidationResult result) {
        validateUniqueness(dto, result);
        validateAdditionalIntegrity(dto, result);
    }

    /**
     * Default uniqueness policy: name must be unique in the catalog table.
     * Override this hook when uniqueness depends on an additional scope.
     */
    protected void validateUniqueness(D dto, CrudValidationResult result) {
        long id = dto.id() == null ? 0L : dto.id();
        if (repository.existsByNameAndIdNot(dto.name(), id)) {
            result.addError("name", CatalogMessageKeys.NAME_DUPLICATE,
                    Map.of("0", entityName(), "1", dto.name()));
        }
    }

    protected void validateNameMutability(D dto, CrudValidationResult result) {
        if (isNameMutable() || dto.id() == null) {
            return;
        }

        repository.findById(dto.id())
                .map(BaseCatalogEntity.class::cast)
                .ifPresent(entity -> {
                    if (!Objects.equals(entity.getName(), dto.name())) {
                        result.addError(
                                "name",
                                CatalogMessageKeys.NAME_IMMUTABLE,
                                Map.of(
                                        "0", entityName(),
                                        "1", entity.getName()
                                )
                        );
                    }
                });
    }

    protected boolean isNameMutable() {
        return false;
    }

    protected void validateSettings(D dto, CrudValidationResult result) {
    }

    protected void validateAdditionalFields(D dto, CrudValidationResult result) {
    }

    protected void validateAdditionalCatalogFields(D dto, CrudValidationResult result) {
    }

    protected void validateAdditionalIntegrity(D dto, CrudValidationResult result) {
    }
}
