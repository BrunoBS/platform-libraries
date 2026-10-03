package br.com.portalmanager.platform.library.catalog.validation;

import br.com.portalmanager.platform.library.catalog.dto.CatalogDTO;
import br.com.portalmanager.platform.library.catalog.message.CatalogMessageKeys;
import br.com.portalmanager.platform.library.catalog.repository.CatalogRepository;
import br.com.portalmanager.platform.library.messaging.exception.ValidationException;
import br.com.portalmanager.platform.library.messaging.message.PlatformMessageKeys;
import br.com.portalmanager.platform.library.messaging.validation.ValidationResult;

import java.util.Map;
import java.util.regex.Pattern;

public abstract class AbstractCatalogValidator {

    public static final String CODE_FORMAT = "^[A-Z][A-Z0-9_]{0,49}$";
    private static final int LABEL_MAX_LENGTH = 100;
    private static final Pattern CODE_PATTERN = Pattern.compile(CODE_FORMAT);
    private final CatalogRepository<?> repository;

    protected AbstractCatalogValidator(CatalogRepository<?> repository) {
        this.repository = repository;
    }

    public void validateForCreate(CatalogDTO dto) {
        ValidationResult result = new ValidationResult();
        validateRequired(dto, result);
        if (!result.hasErrors()) {
            validateAttributes(dto, result);
            validateUniqueness(dto, result);
        }
        throwIfInvalid(result);
    }

    public void validateForUpdate(CatalogDTO dto) {
        ValidationResult result = new ValidationResult();
        validateRequired(dto, result);
        if (!result.hasErrors()) validateAttributes(dto, result);
        throwIfInvalid(result);
    }

    public void validateForDelete(CatalogDTO dto) {
        ValidationResult result = new ValidationResult();
        validateRequired(dto, result);
        if (!result.hasErrors()) validateCode(dto, result);
        throwIfInvalid(result);
    }

    private void validateRequired(CatalogDTO dto, ValidationResult result) {
        if (dto == null) result.addError(entityName(), CatalogMessageKeys.REQUIRED);
    }

    private void validateAttributes(CatalogDTO dto, ValidationResult result) {
        validateCode(dto, result);
        if (dto.label() == null || dto.label().isBlank()) {
            result.addError("label", CatalogMessageKeys.LABEL_REQUIRED, Map.of("0", entityName()));
        } else if (dto.label().length() > LABEL_MAX_LENGTH) {
            result.addError("label", CatalogMessageKeys.LABEL_INVALID_LENGTH,
                    Map.of("0", entityName(), "1", LABEL_MAX_LENGTH));
        }
        if (dto.description() == null || dto.description().isBlank()) {
            result.addError("description", CatalogMessageKeys.DESCRIPTION_REQUIRED, Map.of("0", entityName()));
        } else if (dto.description().length() < 3 || dto.description().length() > 250) {
            result.addError("description", CatalogMessageKeys.DESCRIPTION_INVALID_LENGTH,
                    Map.of("0", entityName(), "1", 3, "2", 250));
        }
        validateSettings(dto, result);
        validateCatalogFields(dto, result);
    }

    protected void validateCode(CatalogDTO dto, ValidationResult result) {
        if (dto.code() == null || dto.code().isBlank()) {
            result.addError("code", CatalogMessageKeys.CODE_REQUIRED, Map.of("0", entityName()));
        } else if (!CODE_PATTERN.matcher(dto.code()).matches()) {
            result.addError("code", CatalogMessageKeys.CODE_INVALID_FORMAT,
                    Map.of("0", entityName(), "1", CODE_FORMAT));
        }
    }

    private void validateUniqueness(CatalogDTO dto, ValidationResult result) {
        if (dto.code() != null && repository.existsById(dto.code())) {
            result.addError("code", CatalogMessageKeys.CODE_DUPLICATE,
                    Map.of("0", entityName(), "1", dto.code()));
        }
    }

    private void throwIfInvalid(ValidationResult result) {
        if (result.hasErrors()) {
            throw new ValidationException(PlatformMessageKeys.VALIDATION_FAILED, result.getDetails());
        }
    }

    protected void validateSettings(CatalogDTO dto, ValidationResult result) {}
    protected void validateCatalogFields(CatalogDTO dto, ValidationResult result) {}
    public abstract String entityName();
}
