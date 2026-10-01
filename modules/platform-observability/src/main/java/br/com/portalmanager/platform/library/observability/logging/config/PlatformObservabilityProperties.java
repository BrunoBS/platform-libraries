package br.com.portalmanager.platform.library.observability.logging.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.util.unit.DataSize;

import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;

@ConfigurationProperties(prefix = "platform.observability")
public class PlatformObservabilityProperties {

    private Logging logging = new Logging();

    public Logging getLogging() { return logging; }
    public void setLogging(Logging logging) { this.logging = logging == null ? new Logging() : logging; }

    public static class Logging {
        private Masking masking = new Masking();
        private RequestBody requestBody = new RequestBody();
        private Map<String, String> defaults = new LinkedHashMap<>();
        private Map<String, String> levels = new LinkedHashMap<>();
        private Map<String, String> customConverters = new LinkedHashMap<>();

        public Masking getMasking() { return masking; }
        public void setMasking(Masking masking) { this.masking = masking == null ? new Masking() : masking; }
        public RequestBody getRequestBody() { return requestBody; }
        public void setRequestBody(RequestBody requestBody) { this.requestBody = requestBody == null ? new RequestBody() : requestBody; }
        public Map<String, String> getDefaults() { return defaults; }
        public void setDefaults(Map<String, String> defaults) { this.defaults = defaults == null ? new LinkedHashMap<>() : new LinkedHashMap<>(defaults); }
        public Map<String, String> getLevels() { return levels; }
        public void setLevels(Map<String, String> levels) { this.levels = levels == null ? new LinkedHashMap<>() : new LinkedHashMap<>(levels); }
        public Map<String, String> getCustomConverters() { return customConverters; }
        public void setCustomConverters(Map<String, String> customConverters) { this.customConverters = customConverters == null ? new LinkedHashMap<>() : new LinkedHashMap<>(customConverters); }
    }

    public static class Masking {
        private boolean enabled = true;
        private Set<String> additionalSensitiveFields = new LinkedHashSet<>();

        public boolean isEnabled() { return enabled; }
        public void setEnabled(boolean enabled) { this.enabled = enabled; }
        public Set<String> getAdditionalSensitiveFields() { return additionalSensitiveFields; }
        public void setAdditionalSensitiveFields(Set<String> additionalSensitiveFields) {
            this.additionalSensitiveFields = additionalSensitiveFields == null
                    ? new LinkedHashSet<>()
                    : new LinkedHashSet<>(additionalSensitiveFields);
        }
    }

    public static class RequestBody {
        private boolean enabled = false;
        private DataSize maxSize = DataSize.ofMegabytes(1);

        public boolean isEnabled() { return enabled; }
        public void setEnabled(boolean enabled) { this.enabled = enabled; }
        public DataSize getMaxSize() { return maxSize; }
        public void setMaxSize(DataSize maxSize) { this.maxSize = maxSize == null ? DataSize.ofMegabytes(1) : maxSize; }
    }
}
