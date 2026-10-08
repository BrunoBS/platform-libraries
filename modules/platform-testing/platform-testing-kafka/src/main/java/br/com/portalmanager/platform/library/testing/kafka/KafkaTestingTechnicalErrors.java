package br.com.portalmanager.platform.library.testing.kafka;

import br.com.portalmanager.platform.library.messaging.model.PlatformErrorDefinition;

public final class KafkaTestingTechnicalErrors {

    public static PlatformErrorDefinition invalidKafkaConfiguration(String detail) {
        return new PlatformErrorDefinition(
                "PLT-TST-003",
                "Invalid platform-testing Kafka configuration: " + detail,
                "undefined",
                500
        );
    }

    private KafkaTestingTechnicalErrors() {
    }
}
