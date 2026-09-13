package com.empresa.platform.catalog.validation;

import com.empresa.platform.catalog.dto.BaseCatalogDTO;
import com.empresa.platform.catalog.message.CatalogMessageKeys;
import com.empresa.platform.catalog.repository.BaseCatalogRepository;
import com.empresa.platform.crud.validation.CrudValidationResult;

import java.util.Map;

/**
 * Generic validator for managed catalogs. It does not assume a Java enum or
 * any fixed set of allowed names; the database may be the source of truth.
 *
 * Name uniqueness is provided as the default policy but may be overridden by
 * scoped catalogs, for example account + name or application + name.
 */
public abstract class BaseCatalogValidator<D extends BaseCatalogDTO<D>> extends BaseValidator<D> {

    protected final BaseCatalogRepository<?> repository;

    protected BaseCatalogValidator(BaseCatalogRepository<?> repository) {
        this.repository = repository;
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

    protected void validateSettings(D dto, CrudValidationResult result) {
    }

    protected void validateAdditionalCatalogFields(D dto, CrudValidationResult result) {
    }

    protected void validateAdditionalIntegrity(D dto, CrudValidationResult result) {
    }
}
