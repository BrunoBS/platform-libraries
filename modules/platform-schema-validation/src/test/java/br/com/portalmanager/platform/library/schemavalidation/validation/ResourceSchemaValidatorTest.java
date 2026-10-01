package br.com.portalmanager.platform.library.schemavalidation.validation;

import br.com.portalmanager.platform.library.schemavalidation.model.ResourceSchema;
import br.com.portalmanager.platform.library.schemavalidation.resolver.ResourceSchemaResolver;
import org.junit.jupiter.api.Test;
import tools.jackson.databind.ObjectMapper;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class ResourceSchemaValidatorTest {

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Test
    void shouldReuseCompiledSchemaForSamePublishedVersion() {
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
    void shouldCompileAnotherSchemaWhenPublishedVersionChanges() {
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
                  }
                }
                """
        );
        when(resolver.resolve("APPLICATION", "application"))
                .thenReturn(versionOne)
                .thenReturn(versionTwo);

        ResourceSchemaValidator validator = new ResourceSchemaValidator(resolver, objectMapper);

        validator.validate("APPLICATION", "application", objectMapper.createObjectNode());
        validator.validate(
                "APPLICATION",
                "application",
                objectMapper.valueToTree(new SampleInput("new-version"))
        );

        verify(resolver, times(2)).resolve("APPLICATION", "application");
    }

    record SampleInput(String name) {
    }
}
