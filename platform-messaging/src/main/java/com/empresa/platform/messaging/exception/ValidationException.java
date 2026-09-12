package com.empresa.platform.messaging.exception;

import com.empresa.platform.messaging.model.ValidationDetail;

import java.util.List;
import java.util.Map;

public class ValidationException extends ApiException {

    private final List<ValidationDetail> details;

    public ValidationException(String messageKey) {
        this(messageKey, Map.of(), List.of(), null);
    }

    public ValidationException(String messageKey, Throwable cause) {
        this(messageKey, Map.of(), List.of(), cause);
    }

    public ValidationException(String messageKey, Map<String, Object> parameters) {
        this(messageKey, parameters, List.of(), null);
    }

    public ValidationException(
            String messageKey,
            Map<String, Object> parameters,
            Throwable cause
    ) {
        this(messageKey, parameters, List.of(), cause);
    }

    public ValidationException(
            String messageKey,
            List<ValidationDetail> details
    ) {
        this(messageKey, Map.of(), details, null);
    }

    public ValidationException(
            String messageKey,
            Map<String, Object> parameters,
            List<ValidationDetail> details
    ) {
        this(messageKey, parameters, details, null);
    }

    public ValidationException(
            String messageKey,
            Map<String, Object> parameters,
            List<ValidationDetail> details,
            Throwable cause
    ) {
        super(messageKey, parameters, cause);
        this.details = details == null ? List.of() : List.copyOf(details);
    }

    public List<ValidationDetail> getDetails() {
        return details;
    }
}
