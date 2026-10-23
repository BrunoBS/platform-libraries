package br.com.portalmanager.platform.library.schemavalidation.validation;

import static org.assertj.core.api.Assertions.*;

import br.com.portalmanager.platform.library.messaging.exception.PlatformConfigurationException;
import br.com.portalmanager.platform.library.messaging.exception.ValidationException;
import br.com.portalmanager.platform.library.messaging.validation.ValidationResult;
import br.com.portalmanager.platform.library.schemavalidation.cache.CaffeineCompiledSchemaCache;
import br.com.portalmanager.platform.library.schemavalidation.model.ResourceSchema;

import org.junit.jupiter.api.Test;

import tools.jackson.databind.json.JsonMapper;

import java.time.Duration;

class JsonSchemaValidatorTest {
    private final JsonMapper mapper = JsonMapper.builder().build();
    private final JsonSchemaValidator validator =
            new JsonSchemaValidator(
                    mapper, new CaffeineCompiledSchemaCache(Duration.ofMinutes(1), 10));
    private static final String UUID_SCHEMA =
            "{\"$schema\":\"https://json-schema.org/draft/2020-12/schema\",\"type\":\"object\",\"additionalProperties\":false,\"required\":[\"identifier\"],\"properties\":{\"identifier\":{\"type\":\"string\",\"format\":\"uuid\"}}}";

    @Test
    void httpAndProgrammaticValidationAgreeOnFormats() {
        ResourceSchema schema = schema(UUID_SCHEMA, 1);
        ResourceSchemaValidator http =
                new ResourceSchemaValidator((type, code) -> schema, validator);
        assertThatThrownBy(
                        () ->
                                http.validate(
                                        "PLATFORM",
                                        "probe",
                                        mapper.createObjectNode()
                                                .put("identifier", "invalid-uuid")))
                .isInstanceOf(ValidationException.class);
        assertThatThrownBy(
                        () ->
                                validator.validate(
                                        schema,
                                        mapper.createObjectNode()
                                                .put("identifier", "invalid-uuid")))
                .isInstanceOf(ValidationException.class);
        assertThatCode(
                        () ->
                                http.validate(
                                        "PLATFORM",
                                        "probe",
                                        mapper.createObjectNode()
                                                .put(
                                                        "identifier",
                                                        "a519440c-1b6c-41ad-8da9-4f304b4141ac")))
                .doesNotThrowAnyException();
        assertThatCode(
                        () ->
                                validator.validate(
                                        schema,
                                        mapper.createObjectNode()
                                                .put(
                                                        "identifier",
                                                        "a519440c-1b6c-41ad-8da9-4f304b4141ac")))
                .doesNotThrowAnyException();
    }

    @Test
    void validatesDefinitionsWithoutTreatingThemAsInstances() {
        assertThatCode(
                        () ->
                                validator.validateDefinition(
                                        mapper.readTree(UUID_SCHEMA), "definition"))
                .doesNotThrowAnyException();
        assertThatCode(() -> validator.validateDefinition(mapper.readTree("false"), "definition"))
                .doesNotThrowAnyException();
        assertThatThrownBy(
                        () ->
                                validator.validateDefinition(
                                        mapper.readTree("{\"type\":\"unknown-type\"}"),
                                        "definition"))
                .isInstanceOf(ValidationException.class);
        assertThatThrownBy(() -> validator.validateDefinition(mapper.nullNode(), "definition"))
                .isInstanceOf(ValidationException.class);
    }

    @Test
    void missingAndMalformedPublishedSchemasFailClosed() {
        for (String definition :
                new String[] {null, "", "null", "{broken", "{\"type\":\"unknown-type\"}"}) {
            assertThatThrownBy(
                            () ->
                                    validator.validate(
                                            schema(definition, 1), mapper.createObjectNode()))
                    .isInstanceOf(PlatformConfigurationException.class);
        }
        assertThatThrownBy(() -> validator.validate(null, mapper.createObjectNode()))
                .isInstanceOf(PlatformConfigurationException.class);
    }

    @Test
    void rejectsNullExtraFieldsAndMissingRequiredFieldsWithConsistentErrors() {
        ResourceSchema schema = schema(UUID_SCHEMA, 1);
        assertThatThrownBy(() -> validator.validate(schema, mapper.nullNode()))
                .isInstanceOf(ValidationException.class);
        assertThatThrownBy(() -> validator.validate(schema, mapper.createObjectNode()))
                .isInstanceOf(ValidationException.class);
        assertThatThrownBy(
                        () ->
                                validator.validate(
                                        schema,
                                        mapper.createObjectNode()
                                                .put(
                                                        "identifier",
                                                        "a519440c-1b6c-41ad-8da9-4f304b4141ac")
                                                .put("extra", true)))
                .isInstanceOf(ValidationException.class);
        ValidationResult result = new ValidationResult();
        validator.validate(
                schema,
                mapper.createObjectNode().put("identifier", "invalid-uuid"),
                "publishers[0].parameters",
                result);
        assertThat(result.hasErrors()).isTrue();
        assertThat(result.getDetails())
                .singleElement()
                .satisfies(
                        detail ->
                                assertThat(detail.field())
                                        .isEqualTo("publishers[0].parameters.identifier"));
    }

    @Test
    void cacheSeparatesVersionsAndDefinitionContent() {
        assertThatCode(
                        () ->
                                validator.validate(
                                        schema("{\"type\":\"object\"}", 1),
                                        mapper.createObjectNode()))
                .doesNotThrowAnyException();
        assertThatThrownBy(
                        () -> validator.validate(schema(UUID_SCHEMA, 2), mapper.createObjectNode()))
                .isInstanceOf(ValidationException.class);
        assertThatThrownBy(
                        () -> validator.validate(schema(UUID_SCHEMA, 1), mapper.createObjectNode()))
                .isInstanceOf(ValidationException.class);
    }

    @Test
    void unresolvedReferencesAreControlledTechnicalFailures() {
        assertThatThrownBy(
                        () ->
                                validator.validate(
                                        schema("{\"$ref\":\"#/$defs/missing\"}", 1),
                                        mapper.createObjectNode()))
                .isInstanceOf(PlatformConfigurationException.class);
    }

    private ResourceSchema schema(String definition, int version) {
        return new ResourceSchema("PLATFORM", "probe", version, definition);
    }
}
