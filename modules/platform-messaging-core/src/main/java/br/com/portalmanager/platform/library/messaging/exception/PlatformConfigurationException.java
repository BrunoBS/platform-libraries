package br.com.portalmanager.platform.library.messaging.exception;

import br.com.portalmanager.platform.library.messaging.model.ApiErrorResponse;
import br.com.portalmanager.platform.library.messaging.model.PlatformErrorDefinition;

import java.time.Instant;

public class PlatformConfigurationException extends IllegalStateException {

    private final ApiErrorResponse errorResponse;

    public PlatformConfigurationException(PlatformErrorDefinition definition) {
        this(definition, null);
    }

    public PlatformConfigurationException(
            PlatformErrorDefinition definition,
            Throwable cause
    ) {
        super(definition.message(), cause);
        this.errorResponse = new ApiErrorResponse(
                definition.code(),
                definition.message(),
                definition.solution(),
                Instant.now(),
                null,
                null
        );
    }

    public ApiErrorResponse getErrorResponse() {
        return errorResponse;
    }
}
