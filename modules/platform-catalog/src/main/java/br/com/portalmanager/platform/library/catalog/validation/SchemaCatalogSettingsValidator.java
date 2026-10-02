package br.com.portalmanager.platform.library.catalog.validation;

import br.com.portalmanager.platform.library.catalog.dto.CatalogDTO;
import br.com.portalmanager.platform.library.messaging.exception.ValidationException;
import br.com.portalmanager.platform.library.messaging.model.ValidationDetail;
import br.com.portalmanager.platform.library.messaging.validation.ValidationResult;
import br.com.portalmanager.platform.library.schemavalidation.validation.SchemaValidator;

import java.util.List;

final class SchemaCatalogSettingsValidator implements CatalogSettingsValidator {

    private static final String RESOURCE_TYPE = "CATALOG";
    private static final String SETTINGS_FIELD = "settings";

    private final String resourceCode;
    private final SchemaValidator schemaValidator;

    SchemaCatalogSettingsValidator(String resourceCode, SchemaValidator schemaValidator) {
        this.resourceCode = resourceCode;
        this.schemaValidator = schemaValidator;
    }

    @Override
    public void validate(CatalogDTO dto, ValidationResult result) {
        try {
            schemaValidator.validate(RESOURCE_TYPE, resourceCode, dto.settings());
        } catch (ValidationException exception) {
            result.mergeDetails(prefixSettings(exception.getDetails()));
        }
    }

    private List<ValidationDetail> prefixSettings(List<ValidationDetail> details) {
        return details.stream()
                .map(detail -> new ValidationDetail(
                        prefixSettings(detail.field()),
                        detail.messageKey(),
                        detail.parameters(),
                        detail.defaultMessage(),
                        detail.fallbackMessageKey()
                ))
                .toList();
    }

    private String prefixSettings(String field) {
        if (field == null || field.isBlank() || "request".equals(field)) {
            return SETTINGS_FIELD;
        }
        return field.startsWith("[")
                ? SETTINGS_FIELD + field
                : SETTINGS_FIELD + "." + field;
    }
}
