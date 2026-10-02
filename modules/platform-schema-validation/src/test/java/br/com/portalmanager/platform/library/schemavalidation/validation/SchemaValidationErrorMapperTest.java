package br.com.portalmanager.platform.library.schemavalidation.validation;

import br.com.portalmanager.platform.library.schemavalidation.message.SchemaValidationMessageKeys;
import com.networknt.schema.Error;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class SchemaValidationErrorMapperTest {

    private final SchemaValidationErrorMapper mapper = new SchemaValidationErrorMapper();

    @Test
    void shouldMapKnownKeywordsToPlatformMessageKeys() {
        assertKey("required", SchemaValidationMessageKeys.REQUIRED);
        assertKey("type", SchemaValidationMessageKeys.TYPE);
        assertKey("minLength", SchemaValidationMessageKeys.MIN_LENGTH);
        assertKey("maxLength", SchemaValidationMessageKeys.MAX_LENGTH);
        assertKey("minimum", SchemaValidationMessageKeys.MINIMUM);
        assertKey("maximum", SchemaValidationMessageKeys.MAXIMUM);
        assertKey("pattern", SchemaValidationMessageKeys.PATTERN);
        assertKey("enum", SchemaValidationMessageKeys.ENUM);
        assertKey("additionalProperties", SchemaValidationMessageKeys.ADDITIONAL_PROPERTIES);
    }

    @Test
    void shouldFallbackToGenericMessageForUnknownKeyword() {
        assertKey("futureKeyword", SchemaValidationMessageKeys.INVALID);
    }

    private void assertKey(String keyword, String expectedKey) {
        Error error = mock(Error.class);
        when(error.getKeyword()).thenReturn(keyword);

        var mapped = mapper.map(error, "name");

        assertEquals("name", mapped.field());
        assertEquals(expectedKey, mapped.messageKey());
    }
}
