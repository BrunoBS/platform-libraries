package br.com.portalmanager.platform.library.objectstorage.api.configuration;

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
        ObjectStorageConfigurationValidation.validate(this);
    }

    public static class Destination {
        private String bucket;
        private String container;
        private String prefix = "";
        private String suffix = "";

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

        public String getSuffix() {
            return suffix;
        }

        public void setSuffix(String suffix) {
            this.suffix = suffix;
        }
    }
}
