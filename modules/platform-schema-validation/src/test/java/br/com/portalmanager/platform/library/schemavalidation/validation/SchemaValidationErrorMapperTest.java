package br.com.portalmanager.platform.library.schemavalidation.validation;

import br.com.portalmanager.platform.library.schemavalidation.message.SchemaValidationMessageKeys;
import com.networknt.schema.Error;
import org.junit.jupiter.api.Test;

import java.util.Map;

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

    @Test
    void shouldMapExpectedTypeInsteadOfActualType() {
        SchemaValidationErrorMapper.MappedValidationError mapped =
                map("type", "name", "integer", "string");

        assertEquals(Map.of("0", "name", "1", "string"), mapped.parameters());
    }

    @Test
    void shouldMapStableLimitParameter() {
        SchemaValidationErrorMapper.MappedValidationError mapped =
                map("minLength", "name", 3);

        assertEquals(Map.of("0", "name", "1", "3"), mapped.parameters());
    }

    @Test
    void shouldMapExpectedConstValue() {
        SchemaValidationErrorMapper.MappedValidationError mapped =
                map("const", "status", "ACTIVE", "INACTIVE");

        assertEquals(Map.of("0", "status", "1", "ACTIVE"), mapped.parameters());
    }

    @Test
    void shouldMapDependentRequiredUsingMissingAndOriginFields() {
        SchemaValidationErrorMapper.MappedValidationError mapped =
                map("dependentRequired", "address", "zipCode", "address");

        assertEquals("zipCode", mapped.field());
        assertEquals(Map.of("0", "zipCode", "1", "address"), mapped.parameters());
    }

    @Test
    void shouldKeepTechnicalArgumentsOutOfGenericMessages() {
        SchemaValidationErrorMapper.MappedValidationError mapped =
                map("pattern", "name", "^[A-Z]+$");

        assertEquals(Map.of("0", "name"), mapped.parameters());
    }

    private void assertKey(String keyword, String expectedKey) {
        SchemaValidationErrorMapper.MappedValidationError mapped = map(keyword, "name");

        assertEquals("name", mapped.field());
        assertEquals(expectedKey, mapped.messageKey());
        assertEquals(Map.of("0", "name"), mapped.parameters());
    }

    private SchemaValidationErrorMapper.MappedValidationError map(
            String keyword,
            String field,
            Object... arguments
    ) {
        Error error = mock(Error.class);
        when(error.getKeyword()).thenReturn(keyword);
        when(error.getArguments()).thenReturn(arguments.length == 0 ? null : arguments);

        return mapper.map(error, field);
    }
}
