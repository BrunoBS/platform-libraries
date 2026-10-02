package br.com.portalmanager.platform.library.schemavalidation.validation;

import br.com.portalmanager.platform.library.schemavalidation.message.SchemaValidationMessageKeys;
import com.networknt.schema.Error;

final class SchemaValidationErrorMapper {

    MappedValidationError map(Error error, String field) {
        return new MappedValidationError(
                field,
                SchemaValidationMessageKeys.fromKeyword(error.getKeyword())
        );
    }

    record MappedValidationError(String field, String messageKey) {
    }
}
