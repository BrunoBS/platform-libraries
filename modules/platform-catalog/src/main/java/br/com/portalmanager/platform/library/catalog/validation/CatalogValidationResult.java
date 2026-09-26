package br.com.portalmanager.platform.library.catalog.validation;

import br.com.portalmanager.platform.library.messaging.model.ValidationDetail;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public final class CatalogValidationResult {
    private final List<ValidationDetail> details = new ArrayList<>();
    public void addError(String field, String messageKey) { addError(field, messageKey, Map.of()); }
    public void addError(String field, String messageKey, Map<String, Object> parameters) {
        details.add(new ValidationDetail(field, messageKey, parameters));
    }
    public boolean hasErrors() { return !details.isEmpty(); }
    public List<ValidationDetail> getDetails() { return List.copyOf(details); }
}
