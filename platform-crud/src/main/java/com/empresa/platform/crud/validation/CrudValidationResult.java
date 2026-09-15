package com.empresa.platform.crud.validation;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class CrudValidationResult {

    private final List<CrudValidationDetail> details = new ArrayList<>();

    public void addError(String field, String messageKey) {
        if (isMessageKey(messageKey)) {
            details.add(new CrudValidationDetail(field, messageKey));
            return;
        }
        details.add(CrudValidationDetail.literal(field, messageKey));
    }

    public void addError(
            String field,
            String messageKey,
            Map<String, Object> parameters) {
        details.add(new CrudValidationDetail(field, messageKey, parameters));
    }

    public boolean hasErrors() {
        return !details.isEmpty();
    }

    public List<CrudValidationDetail> getDetails() {
        return List.copyOf(details);
    }

    public void merge(CrudValidationResult other) {
        if (other != null) {
            details.addAll(other.details);
        }
    }

    private boolean isMessageKey(String value) {
        return value != null && value.matches("^[a-z0-9._-]+$");
    }
}

