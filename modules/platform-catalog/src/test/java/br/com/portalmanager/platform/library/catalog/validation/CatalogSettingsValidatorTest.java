package br.com.portalmanager.platform.library.catalog.validation;

import br.com.portalmanager.platform.library.catalog.dto.CatalogDTO;
import br.com.portalmanager.platform.library.messaging.exception.ValidationException;
import br.com.portalmanager.platform.library.messaging.model.ValidationDetail;
import br.com.portalmanager.platform.library.messaging.validation.ValidationResult;
import br.com.portalmanager.platform.library.schemavalidation.validation.SchemaValidator;
import org.junit.jupiter.api.Test;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class CatalogSettingsValidatorTest {

    private static final JsonMapper JSON = JsonMapper.builder().build();

    @Test
    void shouldValidateSettingsAsCatalogResource() {
        RecordingSchemaValidator schemaValidator = new RecordingSchemaValidator();
        CatalogDTO dto = dto();

        CatalogSettingsValidator.schema("workspace-type", schemaValidator)
                .validate(dto, new ValidationResult());

        assertThat(schemaValidator.resourceType).isEqualTo("CATALOG");
        assertThat(schemaValidator.resourceCode).isEqualTo("workspace-type");
        assertThat(schemaValidator.payload).isSameAs(dto.settings());
    }

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

    @Test
    void shouldNotHideTechnicalSchemaErrors() {
        SchemaValidator schemaValidator = (type, code, payload) -> {
            throw new IllegalStateException("schema unavailable");
        };

        assertThatThrownBy(() -> CatalogSettingsValidator.schema("workspace-type", schemaValidator)
                .validate(dto(), new ValidationResult()))
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("schema unavailable");
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

    private static final class RecordingSchemaValidator implements SchemaValidator {
        private String resourceType;
        private String resourceCode;
        private JsonNode payload;

        @Override
        public void validate(String resourceType, String resourceCode, JsonNode payload) {
            this.resourceType = resourceType;
            this.resourceCode = resourceCode;
            this.payload = payload;
        }
    }
}
