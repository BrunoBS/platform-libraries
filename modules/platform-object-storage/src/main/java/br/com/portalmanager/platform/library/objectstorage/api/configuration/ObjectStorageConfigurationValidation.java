package br.com.portalmanager.platform.library.objectstorage.api.configuration;

import br.com.portalmanager.platform.library.messaging.api.exception.PlatformConfigurationException;
import br.com.portalmanager.platform.library.messaging.api.exception.ValidationException;
import java.util.Map;

/** Validates the consumer-facing storage configuration without depending on provider internals. */
final class ObjectStorageConfigurationValidation {
    private ObjectStorageConfigurationValidation() { }

    static void validate(ObjectStorageProperties properties) {
        if (properties.getProvider() == null) {
            throw invalidConfiguration("platform.object-storage.provider is required");
        }
        Map<String, ObjectStorageProperties.Destination> destinations = properties.getDestinations();
        if (destinations == null || destinations.isEmpty()) {
            throw invalidConfiguration("At least one destination must be configured");
        }
        if (properties.getProvider() == ObjectStorageProvider.AWS
                && (properties.getRegion() == null || properties.getRegion().isBlank())) {
            throw invalidConfiguration("platform.object-storage.region is required for AWS");
        }
        if (properties.getProvider() == ObjectStorageProvider.AZURE
                && (properties.getEndpoint() == null || properties.getEndpoint().isBlank())) {
            throw invalidConfiguration("platform.object-storage.endpoint is required for AZURE");
        }
        for (Map.Entry<String, ObjectStorageProperties.Destination> entry : destinations.entrySet()) {
            ObjectStorageProperties.Destination destination = entry.getValue();
            if (entry.getKey() == null || entry.getKey().isBlank() || destination == null) {
                throw invalidConfiguration("Destination names and values must be present");
            }
            String physicalName = properties.getProvider() == ObjectStorageProvider.AWS
                    ? destination.getBucket() : destination.getContainer();
            if (physicalName == null || physicalName.isBlank()) {
                String propertyName = properties.getProvider() == ObjectStorageProvider.AWS ? "bucket" : "container";
                throw invalidConfiguration("Destination '" + entry.getKey() + "' is missing " + propertyName);
            }
            try {
                validatePrefix(destination.getPrefix());
                validateSuffix(destination.getSuffix());
            } catch (ValidationException exception) {
                throw invalidConfiguration("Destination '" + entry.getKey() + "' has an invalid prefix or suffix");
            }
        }
    }

    private static void validatePrefix(String prefix) {
        if (prefix == null || prefix.isBlank()) return;
        String normalized = prefix.endsWith("/") ? prefix.substring(0, prefix.length() - 1) : prefix;
        validatePath(normalized, "prefix");
    }

    private static void validateSuffix(String suffix) {
        if (suffix == null || suffix.isEmpty()) return;
        if (suffix.isBlank() || suffix.contains("/") || suffix.contains("\\")
                || suffix.equals(".") || suffix.equals("..")) {
            throw invalid("suffix");
        }
    }

    private static void validatePath(String value, String field) {
        if (value == null || value.isBlank() || value.startsWith("/")
                || value.contains("\\") || value.contains("//")) {
            throw invalid(field);
        }
        for (String segment : value.split("/", -1)) {
            if (segment.isEmpty() || segment.equals(".") || segment.equals("..")) throw invalid(field);
        }
    }

    private static ValidationException invalid(String field) {
        return new ValidationException("objectstorage.request.invalid", Map.of("0", field));
    }

    private static PlatformConfigurationException invalidConfiguration(String reason) {
        return new PlatformConfigurationException(new br.com.portalmanager.platform.library.messaging.api.model.PlatformErrorDefinition(
                "PLT-OS-001", "Invalid platform.object-storage configuration: " + reason,
                "Review the selected provider, credentials, region or endpoint, and logical destinations.", 500));
    }
}
