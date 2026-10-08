package br.com.portalmanager.platform.library.schemavalidation.validation;

import br.com.portalmanager.platform.library.schemavalidation.message.SchemaValidationMessageKeys;
import com.networknt.schema.Error;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
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
        SchemaValidationErrorMapper.MappedValidationError mapped =
                map("futureKeyword", "name", "first", "second");

        assertThat(mapped.messageKey())
                .isEqualTo("schemavalidation.future-keyword");
        assertThat(mapped.parameters())
                .isEqualTo(Map.of("0", "name", "1", "first", "2", "second"));
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

        assertThat(mapped.parameters())
                .isEqualTo(Map.of("0", "name", "1", "string"));
    }

    @Test
    void shouldMapStableLimitParameter() {
        SchemaValidationErrorMapper.MappedValidationError mapped =
                map("minLength", "name", 3);

        assertThat(mapped.parameters())
                .isEqualTo(Map.of("0", "name", "1", "3"));
    }

    @Test
    void shouldMapFormatNameWithoutExposingInvalidValue() {
        SchemaValidationErrorMapper.MappedValidationError mapped =
                map("format", "email", "email", "not-an-email");

        assertThat(mapped.parameters())
                .isEqualTo(Map.of("0", "email", "1", "email"));
    }

    @Test
    void shouldMapExpectedConstValue() {
        SchemaValidationErrorMapper.MappedValidationError mapped =
                map("const", "status", "ACTIVE", "INACTIVE");

        assertThat(mapped.parameters())
                .isEqualTo(Map.of("0", "status", "1", "ACTIVE"));
    }

    @Test
    void shouldMapDependentRequiredUsingMissingAndOriginFields() {
        SchemaValidationErrorMapper.MappedValidationError mapped =
                map("dependentRequired", "address", "zipCode", "address");

        assertThat(mapped.field())
                .isEqualTo("zipCode");
        assertThat(mapped.parameters())
                .isEqualTo(Map.of("0", "zipCode", "1", "address"));
    }

    @Test
    void shouldKeepTechnicalArgumentsOutOfGenericMessages() {
        SchemaValidationErrorMapper.MappedValidationError mapped =
                map("pattern", "name", "^[A-Z]+$");

        assertThat(mapped.parameters())
                .isEqualTo(Map.of("0", "name"));
    }

    private void assertKey(String keyword, String expectedKey) {
        SchemaValidationErrorMapper.MappedValidationError mapped = map(keyword, "name");

        assertThat(mapped.field())
                .isEqualTo("name");
        assertThat(mapped.messageKey())
                .isEqualTo(expectedKey);
        assertThat(mapped.parameters())
                .isEqualTo(Map.of("0", "name"));
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
