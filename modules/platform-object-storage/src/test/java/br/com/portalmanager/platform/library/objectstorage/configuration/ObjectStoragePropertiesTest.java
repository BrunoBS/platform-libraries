package br.com.portalmanager.platform.library.objectstorage.configuration;

import br.com.portalmanager.platform.library.messaging.exception.PlatformConfigurationException;
import java.util.Map;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;

class ObjectStoragePropertiesTest {
    @Test
    void shouldAcceptValidAwsDestinationWithPrefixAndSuffix() {
        ObjectStorageProperties properties = properties("events/", ".json");

        assertDoesNotThrow(properties::afterPropertiesSet);
    }

    @Test
    void shouldRejectPathTraversalInConfiguredPrefix() {
        ObjectStorageProperties properties = properties("events/../private", "");

        assertThrows(PlatformConfigurationException.class, properties::afterPropertiesSet);
    }

    @Test
    void shouldRequireProviderAndDestinationConfiguration() {
        assertThrows(PlatformConfigurationException.class, new ObjectStorageProperties()::afterPropertiesSet);
    }

    private ObjectStorageProperties properties(String prefix, String suffix) {
        ObjectStorageProperties properties = new ObjectStorageProperties();
        properties.setProvider(ObjectStorageProvider.AWS);
        properties.setRegion("us-east-1");
        ObjectStorageProperties.Destination destination = new ObjectStorageProperties.Destination();
        destination.setBucket("object-storage-contract");
        destination.setPrefix(prefix);
        destination.setSuffix(suffix);
        properties.setDestinations(Map.of("audit", destination));
        return properties;
    }
}
