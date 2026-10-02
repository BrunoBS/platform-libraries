package br.com.portalmanager.platform.library.schemavalidation.validation;

import br.com.portalmanager.platform.library.schemavalidation.model.ResourceSchema;
import br.com.portalmanager.platform.library.schemavalidation.resolver.ResourceSchemaResolver;
import br.com.portalmanager.platform.library.messaging.exception.PlatformConfigurationException;
import org.junit.jupiter.api.Test;
import tools.jackson.databind.ObjectMapper;

import static org.junit.jupiter.api.Assertions.assertThrows;
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
        validator.validate(
                "APPLICATION",
                "application",
                objectMapper.valueToTree(new SampleInput("second"))
        );

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

    private void assertInvalidDefinition(String definition) {
        ResourceSchemaResolver resolver = mock(ResourceSchemaResolver.class);
        when(resolver.resolve("APPLICATION", "application"))
                .thenReturn(new ResourceSchema("APPLICATION", "application", 1, definition));

        ResourceSchemaValidator validator = new ResourceSchemaValidator(resolver, objectMapper);

        assertThrows(
                PlatformConfigurationException.class,
                () -> validator.validate(
                        "APPLICATION",
                        "application",
                        objectMapper.valueToTree(new SampleInput("test"))
                )
        );
    }

    record SampleInput(String name) {
    }
}
