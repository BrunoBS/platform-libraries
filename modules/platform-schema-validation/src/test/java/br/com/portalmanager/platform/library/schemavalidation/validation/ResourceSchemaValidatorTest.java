package br.com.portalmanager.platform.library.schemavalidation.validation;

import br.com.portalmanager.platform.library.schemavalidation.model.ResourceSchema;
import br.com.portalmanager.platform.library.schemavalidation.resolver.ResourceSchemaResolver;
import br.com.portalmanager.platform.library.messaging.exception.PlatformConfigurationException;
import br.com.portalmanager.platform.library.messaging.exception.ValidationException;
import br.com.portalmanager.platform.library.schemavalidation.message.SchemaValidationMessageKeys;
import org.junit.jupiter.api.Test;
import tools.jackson.databind.ObjectMapper;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.catchThrowableOfType;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class ResourceSchemaValidatorTest {

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Test
    void shouldResolvePublishedSchemaForEveryValidation() {
        ResourceSchemaResolver resolver = mock(ResourceSchemaResolver.class);
        ResourceSchema schema = new ResourceSchema(
                "APPLICATION",
                "application",
                1,
                """
                {
                  "type": "object",
                  "properties": {
                    "name": { "type": "string" }
                  }
                }
                """
        );
        when(resolver.resolve("APPLICATION", "application")).thenReturn(schema);

        ResourceSchemaValidator validator = new ResourceSchemaValidator(resolver, objectMapper);

        validator.validate(
                "APPLICATION",
                "application",
                objectMapper.valueToTree(new SampleInput("first"))
        );
        validator.validate(
                "APPLICATION",
                "application",
                objectMapper.valueToTree(new SampleInput("second"))
        );

        verify(resolver, times(2)).resolve("APPLICATION", "application");
    }

    @Test
    void shouldUseNewDefinitionWhenPublishedVersionChanges() {
        ResourceSchemaResolver resolver = mock(ResourceSchemaResolver.class);
        ResourceSchema versionOne = new ResourceSchema(
                "APPLICATION", "application", 1, "{\"type\":\"object\"}"
        );
        ResourceSchema versionTwo = new ResourceSchema(
                "APPLICATION", "application", 2,
                """
                {
                  "type": "object",
                  "properties": {
                    "name": { "type": "string" }
                  },
                  "required": ["name"]
                }
                """
        );
        when(resolver.resolve("APPLICATION", "application"))
                .thenReturn(versionOne)
                .thenReturn(versionTwo);

        ResourceSchemaValidator validator = new ResourceSchemaValidator(resolver, objectMapper);

        validator.validate(
                "APPLICATION",
                "application",
                objectMapper.valueToTree(new SampleInput("first"))
        );
        ValidationException exception = catchThrowableOfType(() -> validator.validate(
                        "APPLICATION",
                        "application",
                        objectMapper.createObjectNode()
                ), ValidationException.class);
        assertThat(exception).isNotNull();

        assertThat(exception.getDetails().stream()
                        .anyMatch(detail -> "schemavalidation.required".equals(detail.messageKey())))
                .isTrue();
        verify(resolver, times(2)).resolve("APPLICATION", "application");
    }

    @Test
    void shouldRejectNullPublishedDefinition() {
        assertInvalidDefinition(null);
    }

    @Test
    void shouldRejectBlankPublishedDefinition() {
        assertInvalidDefinition("   ");
    }

    @Test
    void shouldRejectJsonNullPublishedDefinition() {
        assertInvalidDefinition("null");
    }

    @Test
    void shouldRejectMalformedPublishedDefinition() {
        assertInvalidDefinition("{invalid-json");
    }

    @Test
    void shouldMapRealNetworkntRequiredError() {
        assertValidationMessage(
                """
                {"type":"object","required":["name"]}
                """,
                "{}",
                SchemaValidationMessageKeys.fromKeyword("required")
        );
    }

    @Test
    void shouldMapRealNetworkntTypeError() {
        assertValidationMessage(
                """
                {"type":"object","properties":{"name":{"type":"string"}}}
                """,
                """
                {"name":123}
                """,
                SchemaValidationMessageKeys.fromKeyword("type")
        );
    }

    @Test
    void shouldMapRealNetworkntMinLengthError() {
        assertValidationMessage(
                """
                {"type":"object","properties":{"name":{"type":"string","minLength":3}}}
                """,
                """
                {"name":"a"}
                """,
                SchemaValidationMessageKeys.fromKeyword("minLength")
        );
    }

    @Test
    void shouldMapRealNetworkntAdditionalPropertiesError() {
        assertValidationMessage(
                """
                {"type":"object","additionalProperties":false}
                """,
                """
                {"unexpected":"value"}
                """,
                SchemaValidationMessageKeys.fromKeyword("additionalProperties")
        );
    }

    @Test
    void shouldMapRealNetworkntMinimumError() {
        assertValidationMessage(
                "{\"type\":\"number\",\"minimum\":10}",
                "5",
                SchemaValidationMessageKeys.fromKeyword("minimum")
        );
    }

    @Test
    void shouldMapRealNetworkntMinItemsError() {
        assertValidationMessage(
                "{\"type\":\"array\",\"minItems\":2}",
                "[1]",
                SchemaValidationMessageKeys.fromKeyword("minItems")
        );
    }

    @Test
    void shouldMapRealNetworkntMaxLengthError() {
        assertValidationMessage(
                "{\"type\":\"string\",\"maxLength\":3}",
                "\"abcd\"",
                SchemaValidationMessageKeys.fromKeyword("maxLength")
        );
    }

    @Test
    void shouldMapRealNetworkntMultipleOfError() {
        assertValidationMessage(
                "{\"type\":\"number\",\"multipleOf\":2}",
                "3",
                SchemaValidationMessageKeys.fromKeyword("multipleOf")
        );
    }

    @Test
    void shouldMapRealNetworkntUniqueItemsError() {
        assertValidationMessage(
                "{\"type\":\"array\",\"uniqueItems\":true}",
                "[1,1]",
                SchemaValidationMessageKeys.fromKeyword("uniqueItems")
        );
    }

    @Test
    void shouldMapRealNetworkntEnumError() {
        assertValidationMessage(
                "{\"enum\":[\"ACTIVE\",\"INACTIVE\"]}",
                "\"UNKNOWN\"",
                SchemaValidationMessageKeys.fromKeyword("enum")
        );
    }

    @Test
    void shouldMapRealNetworkntConstError() {
        assertValidationMessage(
                "{\"const\":\"EXPECTED\"}",
                "\"OTHER\"",
                SchemaValidationMessageKeys.fromKeyword("const")
        );
    }

    @Test
    void shouldExposeStructuredBranchErrorsForRealNetworkntAnyOf() {
        ResourceSchemaResolver resolver = mock(ResourceSchemaResolver.class);
        when(resolver.resolve("APPLICATION", "application"))
                .thenReturn(new ResourceSchema(
                        "APPLICATION",
                        "application",
                        1,
                        """
                        {
                          "$schema": "https://json-schema.org/draft/2020-12/schema",
                          "anyOf": [
                            { "type": "string" },
                            { "type": "integer" }
                          ]
                        }
                        """
                ));

        ResourceSchemaValidator validator = new ResourceSchemaValidator(resolver, objectMapper);

        ValidationException exception = catchThrowableOfType(() -> validator.validate(
                        "APPLICATION",
                        "application",
                        objectMapper.valueToTree(true)
                ), ValidationException.class);
        assertThat(exception).isNotNull();

        assertThat(exception.getDetails().isEmpty())
                .isFalse();
        assertThat(exception.getDetails().stream()
                        .allMatch(detail -> detail.messageKey().startsWith("schemavalidation.")))
                .isTrue();
        assertThat(exception.getDetails().stream()
                        .allMatch(detail -> SchemaValidationMessageKeys.INVALID.equals(detail.fallbackMessageKey())))
                .isTrue();
    }

    @Test
    void shouldExposeControlledErrorsForRealNetworkntAllOf() {
        assertCompositionProducesOnlyControlledKeys(
                "{\"allOf\":[{\"type\":\"string\"},{\"minLength\":3}]}",
                "1"
        );
    }

    @Test
    void shouldExposeControlledErrorsForRealNetworkntOneOf() {
        assertCompositionProducesOnlyControlledKeys(
                "{\"oneOf\":[{\"type\":\"string\"},{\"type\":\"integer\"}]}",
                "true"
        );
    }

    @Test
    void shouldExposeControlledErrorsForRealNetworkntNot() {
        assertCompositionProducesOnlyControlledKeys(
                "{\"not\":{\"type\":\"string\"}}",
                "\"blocked\""
        );
    }

    @Test
    void shouldRejectInvalidFormatWhenAssertionsAreEnabled() throws Exception {
        ResourceSchemaResolver resolver = mock(ResourceSchemaResolver.class);
        when(resolver.resolve("APPLICATION", "application"))
                .thenReturn(new ResourceSchema(
                        "APPLICATION", "application", 1,
                        "{\"type\":\"string\",\"format\":\"email\"}"
                ));

        ResourceSchemaValidator validator = new ResourceSchemaValidator(resolver, objectMapper);

        ValidationException exception = catchThrowableOfType(() -> validator.validate(
                        "APPLICATION",
                        "application",
                        objectMapper.readTree("\"not-an-email\"")
                ), ValidationException.class);
        assertThat(exception).isNotNull();

        assertThat(exception.getDetails().stream()
                        .anyMatch(detail ->
                                "schemavalidation.format".equals(detail.messageKey())
                                        && "email".equals(detail.parameters().get("1"))
                        ))
                .isTrue();
    }

    private void assertCompositionProducesOnlyControlledKeys(String definition, String payload) {
        ResourceSchemaResolver resolver = mock(ResourceSchemaResolver.class);
        when(resolver.resolve("APPLICATION", "application"))
                .thenReturn(new ResourceSchema("APPLICATION", "application", 1, definition));

        ResourceSchemaValidator validator = new ResourceSchemaValidator(resolver, objectMapper);

        ValidationException exception = catchThrowableOfType(() -> validator.validate(
                        "APPLICATION",
                        "application",
                        objectMapper.readTree(payload)
                ), ValidationException.class);
        assertThat(exception).isNotNull();

        assertThat(exception.getDetails().isEmpty())
                .isFalse();
        assertThat(exception.getDetails().stream()
                        .allMatch(detail -> detail.messageKey().startsWith("schemavalidation.")))
                .isTrue();
        assertThat(exception.getDetails().stream()
                        .allMatch(detail -> SchemaValidationMessageKeys.INVALID.equals(detail.fallbackMessageKey())))
                .isTrue();
    }

    private void assertValidationMessage(String definition, String payload, String expectedMessageKey) {
        ResourceSchemaResolver resolver = mock(ResourceSchemaResolver.class);
        when(resolver.resolve("APPLICATION", "application"))
                .thenReturn(new ResourceSchema("APPLICATION", "application", 1, definition));

        ResourceSchemaValidator validator = new ResourceSchemaValidator(resolver, objectMapper);

        ValidationException exception = catchThrowableOfType(() -> validator.validate(
                        "APPLICATION",
                        "application",
                        objectMapper.readTree(payload)
                ), ValidationException.class);
        assertThat(exception).isNotNull();

        assertThat(exception.getDetails().stream()
                        .anyMatch(detail -> expectedMessageKey.equals(detail.messageKey())))
                .as(() -> "Expected message key " + expectedMessageKey + " but got " +
                        exception.getDetails().stream().map(detail -> detail.messageKey()).toList())
                .isTrue();
    }

    private void assertInvalidDefinition(String definition) {
        ResourceSchemaResolver resolver = mock(ResourceSchemaResolver.class);
        when(resolver.resolve("APPLICATION", "application"))
                .thenReturn(new ResourceSchema("APPLICATION", "application", 1, definition));

        ResourceSchemaValidator validator = new ResourceSchemaValidator(resolver, objectMapper);

        assertThatThrownBy(() -> validator.validate(
                        "APPLICATION",
                        "application",
                        objectMapper.valueToTree(new SampleInput("test"))
                )).isInstanceOf(PlatformConfigurationException.class);
    }

    record SampleInput(String name) {
    }
}
