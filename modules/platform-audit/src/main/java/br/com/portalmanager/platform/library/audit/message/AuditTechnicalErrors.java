package br.com.portalmanager.platform.library.audit.message;

import br.com.portalmanager.platform.library.messaging.model.PlatformErrorDefinition;

public final class AuditTechnicalErrors {

    public static final PlatformErrorDefinition SERVICE_NAME_REQUIRED =
            new PlatformErrorDefinition(
                    "PLT-AUD-003",
                    "platform.audit.service-name is required",
                    "Set platform.audit.service-name to the logical name of the producing service.",
                    500
            );

    private AuditTechnicalErrors() {
    }
}
