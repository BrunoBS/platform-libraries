package br.com.portalmanager.platform.library.objectstorage.message;

import br.com.portalmanager.platform.library.messaging.model.PlatformErrorDefinition;

public final class ObjectStorageTechnicalErrors {
    private ObjectStorageTechnicalErrors() { }

    public static PlatformErrorDefinition invalidConfiguration(String reason) {
        return new PlatformErrorDefinition(
                "PLT-OS-001",
                "Invalid platform.object-storage configuration: " + reason,
                "Review the selected provider, credentials, region or endpoint, and logical destinations.",
                500);
    }
}
