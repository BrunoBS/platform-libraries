package br.com.portalmanager.platform.messaging.exception;

import br.com.portalmanager.platform.messaging.model.ApiErrorResponse;
import br.com.portalmanager.platform.messaging.model.PlatformErrorDefinition;

import java.time.Instant;

public class PlatformConfigurationException extends IllegalStateException {

    private final ApiErrorResponse errorResponse;

    public PlatformConfigurationException(PlatformErrorDefinition definition) {
        super(definition.message());
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
