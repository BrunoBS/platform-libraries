package com.empresa.platform.catalog.validation;

import com.empresa.platform.catalog.dto.BaseTypeDTO;
import com.empresa.platform.catalog.message.CatalogMessageKeys;
import com.empresa.platform.catalog.repository.BaseRepository;
import com.empresa.platform.messaging.validation.ValidationResult;

import java.util.Map;

public abstract class BaseTypeValidator<D extends BaseTypeDTO<D, Long>> extends BaseValidator<D, Long> {

    protected final BaseRepository<?, Long> repository;

    protected BaseTypeValidator(BaseRepository<?, Long> repository) {
        this.repository = repository;
    }

    @Override
    protected void validateAttributes(D dto, ValidationResult result) {
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
    }

    @Override
    protected void validateIntegrity(D dto, ValidationResult result) {
        long id = dto.id() == null ? 0L : dto.id();
        if (repository.existsByNameAndIdNot(dto.name(), id)) {
            result.addError("name", CatalogMessageKeys.NAME_DUPLICATE,
                    Map.of("0", entityName(), "1", dto.name()));
        }
    }

    protected void validateSettings(D dto, ValidationResult result) {
    }
}
