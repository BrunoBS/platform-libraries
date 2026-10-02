package br.com.portalmanager.platform.library.messaging.validation;

import br.com.portalmanager.platform.library.messaging.model.ValidationDetail;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class ValidationResult {

    private final List<ValidationDetail> details = new ArrayList<>();

    public ValidationResult() {
    }

    public ValidationResult(String field, String messageKey) {
        addError(field, messageKey);
    }

    public ValidationResult(
            String field,
            String messageKey,
            Map<String, Object> parameters
    ) {
        addError(field, messageKey, parameters);
    }

    public void addError(String field, String messageKey) {
        details.add(new ValidationDetail(field, messageKey));
    }

    public void addError(
            String field,
            String messageKey,
            Map<String, Object> parameters
    ) {
        details.add(new ValidationDetail(field, messageKey, parameters));
    }

    public void addError(
            String field,
            String messageKey,
            Map<String, Object> parameters,
            String fallbackMessageKey
    ) {
        details.add(new ValidationDetail(field, messageKey, parameters, fallbackMessageKey));
    }

    public void addLiteralError(String field, String message) {
        details.add(ValidationDetail.literal(field, message));
    }

    public boolean hasErrors() {
        return !details.isEmpty();
    }

    public List<ValidationDetail> getDetails() {
        return List.copyOf(details);
    }

    public void merge(ValidationResult other) {
        if (other != null) {
            details.addAll(other.details);
        }
    }

    public void mergeDetails(List<ValidationDetail> validationDetails) {
        if (validationDetails != null && !validationDetails.isEmpty()) {
            details.addAll(validationDetails);
        }
    }

}
