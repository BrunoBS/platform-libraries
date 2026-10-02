package br.com.portalmanager.platform.library.catalog.validation;

import br.com.portalmanager.platform.library.catalog.dto.CatalogDTO;
import br.com.portalmanager.platform.library.messaging.exception.ValidationException;
import br.com.portalmanager.platform.library.messaging.model.ValidationDetail;
import br.com.portalmanager.platform.library.messaging.validation.ValidationResult;
import br.com.portalmanager.platform.library.schemavalidation.validation.SchemaValidator;
import org.junit.jupiter.api.Test;
import tools.jackson.databind.json.JsonMapper;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class CatalogSettingsValidatorTest {

    private static final JsonMapper JSON = JsonMapper.builder().build();

    @Test
    void shouldPrefixSchemaErrorsWithSettingsField() {
        SchemaValidator schemaValidator = (type, code, payload) -> {
            ValidationResult schemaResult = new ValidationResult();
            schemaResult.addError("nested", "schema.invalid");
            throw new ValidationException(schemaResult);
        };

        ValidationResult result = new ValidationResult();

        CatalogSettingsValidator.schema("workspace-type", schemaValidator)
                .validate(dto(), result);

        assertThat(result.getDetails())
                .extracting(ValidationDetail::field)
                .containsExactly("settings.nested");
    }

    @Test
    void shouldMapRootSchemaErrorToSettingsField() {
        SchemaValidator schemaValidator = (type, code, payload) -> {
            ValidationResult schemaResult = new ValidationResult();
            schemaResult.addError("request", "schema.invalid");
            throw new ValidationException(schemaResult);
        };

        ValidationResult result = new ValidationResult();

        CatalogSettingsValidator.schema("workspace-type", schemaValidator)
                .validate(dto(), result);

        assertThat(result.getDetails())
                .extracting(ValidationDetail::field)
                .containsExactly("settings");
    }

    private CatalogDTO dto() {
        return new CatalogDTO(
                "MANAGER",
                "Manager",
                "Descrição válida para catálogo",
                1,
                JSON.createObjectNode()
        );
    }
}
