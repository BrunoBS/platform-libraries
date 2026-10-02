package br.com.portalmanager.platform.library.catalog.validation;

import br.com.portalmanager.platform.library.catalog.dto.CatalogDTOContract;
import br.com.portalmanager.platform.library.catalog.message.CatalogMessageKeys;
import br.com.portalmanager.platform.library.catalog.repository.CatalogRepository;
import br.com.portalmanager.platform.library.messaging.exception.ValidationException;
import br.com.portalmanager.platform.library.messaging.message.PlatformMessageKeys;

import java.util.Map;
import java.util.regex.Pattern;

public abstract class AbstractCatalogValidator<D extends CatalogDTOContract<D>> {

    public static final String CODE_FORMAT = "^[A-Z][A-Z0-9_]{0,49}$";
    private static final Pattern CODE_PATTERN = Pattern.compile(CODE_FORMAT);

    protected final CatalogRepository<?> repository;

    protected AbstractCatalogValidator(CatalogRepository<?> repository) {
        this.repository = repository;
    }

    public void validateForCreate(D dto) {
        CatalogValidationResult result = new CatalogValidationResult();
        validateRequired(dto, result);
        if (!result.hasErrors()) {
            validateAttributes(dto, result);
            validateIntegrity(dto, result);
            validateAdditionalFields(dto, result);
        }
        throwIfInvalid(result);
    }

    public void validateForUpdate(D dto) {
        CatalogValidationResult result = new CatalogValidationResult();
        validateRequired(dto, result);
        if (!result.hasErrors()) {
            validateAttributes(dto, result);
            validateAdditionalFields(dto, result);
        }
        throwIfInvalid(result);
    }

    public void validateForDelete(D dto) {
        CatalogValidationResult result = new CatalogValidationResult();
        validateRequired(dto, result);
        if (!result.hasErrors()) {
            validateCode(dto, result);
            validateDelete(dto, result);
        }
        throwIfInvalid(result);
    }

    protected void validateRequired(D dto, CatalogValidationResult result) {
        if (dto == null) {
            result.addError(entityName(), CatalogMessageKeys.REQUIRED);
        }
    }

    protected void validateAttributes(D dto, CatalogValidationResult result) {
        validateCode(dto, result);

        if (dto.label() == null || dto.label().isBlank()) {
            result.addError("label", CatalogMessageKeys.LABEL_REQUIRED, Map.of("0", entityName()));
        }

        if (dto.description() == null || dto.description().isBlank()) {
            result.addError("description", CatalogMessageKeys.DESCRIPTION_REQUIRED, Map.of("0", entityName()));
        } else if (dto.description().length() < 3 || dto.description().length() > 250) {
            result.addError(
                    "description",
                    CatalogMessageKeys.DESCRIPTION_INVALID_LENGTH,
                    Map.of("0", entityName(), "1", 3, "2", 250)
            );
        }

        validateSettings(dto, result);
        validateAdditionalCatalogFields(dto, result);
    }

    protected void validateCode(D dto, CatalogValidationResult result) {
        if (dto.code() == null || dto.code().isBlank()) {
            result.addError("code", CatalogMessageKeys.CODE_REQUIRED, Map.of("0", entityName()));
            return;
        }

        if (!CODE_PATTERN.matcher(dto.code()).matches()) {
            result.addError(
                    "code",
                    CatalogMessageKeys.CODE_INVALID_FORMAT,
                    Map.of("0", entityName(), "1", CODE_FORMAT)
            );
        }
    }

    protected void validateIntegrity(D dto, CatalogValidationResult result) {
        validateUniqueness(dto, result);
        validateAdditionalIntegrity(dto, result);
    }

    protected void validateUniqueness(D dto, CatalogValidationResult result) {
        if (dto.code() != null && repository.existsById(dto.code())) {
            result.addError(
                    "code",
                    CatalogMessageKeys.CODE_DUPLICATE,
                    Map.of("0", entityName(), "1", dto.code())
            );
        }
    }

    protected void throwIfInvalid(CatalogValidationResult result) {
        if (result.hasErrors()) {
            throw new ValidationException(
                    PlatformMessageKeys.VALIDATION_FAILED,
                    result.getDetails()
            );
        }
    }

    protected void validateDelete(D dto, CatalogValidationResult result) {}
    protected void validateSettings(D dto, CatalogValidationResult result) {}
    protected void validateAdditionalFields(D dto, CatalogValidationResult result) {}
    protected void validateAdditionalCatalogFields(D dto, CatalogValidationResult result) {}
    protected void validateAdditionalIntegrity(D dto, CatalogValidationResult result) {}

    public abstract String entityName();
}
