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
    void shouldDeriveStablePlatformKeysFromNetworkntKeywords() {
        assertKey("required", "schemavalidation.required");
        assertKey("minLength", "schemavalidation.min-length");
        assertKey("additionalProperties", "schemavalidation.additional-properties");
        assertKey("exclusiveMinimum", "schemavalidation.exclusive-minimum");
        assertKey("unevaluatedProperties", "schemavalidation.unevaluated-properties");
    }

    @Test
    void shouldDeriveKeyForFutureKeywordWithoutLibraryChange() {
        assertKey("futureKeyword", "schemavalidation.future-keyword");
    }

    @Test
    void shouldFallbackForMissingOrUnsafeKeyword() {
        assertKey(null, SchemaValidationMessageKeys.INVALID);
        assertKey(" ", SchemaValidationMessageKeys.INVALID);
        assertKey("../unsafe", SchemaValidationMessageKeys.INVALID);
    }

    private void assertKey(String keyword, String expectedKey) {
        Error error = mock(Error.class);
        when(error.getKeyword()).thenReturn(keyword);

        var mapped = mapper.map(error, "name");

        assertEquals("name", mapped.field());
        assertEquals(expectedKey, mapped.messageKey());
    }
}
