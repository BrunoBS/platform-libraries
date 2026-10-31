package br.com.portalmanager.platform.library.objectstorage.configuration;

import br.com.portalmanager.platform.library.messaging.exception.PlatformConfigurationException;
import br.com.portalmanager.platform.library.objectstorage.message.ObjectStorageTechnicalErrors;
import java.util.HashMap;
import java.util.Map;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "platform.object-storage", ignoreUnknownFields = false)
public class ObjectStorageProperties implements InitializingBean {
    private ObjectStorageProvider provider;
    private String region;
    private String endpoint;
    private Map<String, Destination> destinations = new HashMap<>();

    public ObjectStorageProvider getProvider() {
        return provider;
    }

    public void setProvider(ObjectStorageProvider provider) {
        this.provider = provider;
    }

    public String getRegion() {
        return region;
    }

    public void setRegion(String region) {
        this.region = region;
    }

    public String getEndpoint() {
        return endpoint;
    }

    public void setEndpoint(String endpoint) {
        this.endpoint = endpoint;
    }

    public Map<String, Destination> getDestinations() {
        return destinations;
    }

    public void setDestinations(Map<String, Destination> destinations) {
        this.destinations = destinations;
    }

    @Override
    public void afterPropertiesSet() {
        if (provider == null) {
            throw invalidConfiguration("platform.object-storage.provider is required");
        }
        if (destinations == null || destinations.isEmpty()) {
            throw invalidConfiguration("At least one destination must be configured");
        }
        if (provider == ObjectStorageProvider.AWS && (region == null || region.isBlank())) {
            throw invalidConfiguration("platform.object-storage.region is required for AWS");
        }
        if (provider == ObjectStorageProvider.AZURE && (endpoint == null || endpoint.isBlank())) {
            throw invalidConfiguration("platform.object-storage.endpoint is required for AZURE");
        }
        for (Map.Entry<String, Destination> entry : destinations.entrySet()) {
            Destination destination = entry.getValue();
            if (entry.getKey() == null || entry.getKey().isBlank() || destination == null) {
                throw invalidConfiguration("Destination names and values must be present");
            }
            String physicalName = provider == ObjectStorageProvider.AWS
                    ? destination.getBucket()
                    : destination.getContainer();
            if (physicalName == null || physicalName.isBlank()) {
                String propertyName = provider == ObjectStorageProvider.AWS ? "bucket" : "container";
                throw invalidConfiguration("Destination '" + entry.getKey() + "' is missing " + propertyName);
            }
        }
    }

    private PlatformConfigurationException invalidConfiguration(String reason) {
        return new PlatformConfigurationException(ObjectStorageTechnicalErrors.invalidConfiguration(reason));
    }

    public static class Destination {
        private String bucket;
        private String container;
        private String prefix = "";

        public String getBucket() {
            return bucket;
        }

        public void setBucket(String bucket) {
            this.bucket = bucket;
        }

        public String getContainer() {
            return container;
        }

        public void setContainer(String container) {
            this.container = container;
        }

        public String getPrefix() {
            return prefix;
        }

        public void setPrefix(String prefix) {
            this.prefix = prefix;
        }
    }
}
