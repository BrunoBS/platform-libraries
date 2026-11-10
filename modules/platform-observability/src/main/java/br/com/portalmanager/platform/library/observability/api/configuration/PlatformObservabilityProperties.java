package br.com.portalmanager.platform.library.observability.api.configuration;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.LinkedHashMap;
import java.util.Map;

@ConfigurationProperties(prefix = "platform.observability")
public class PlatformObservabilityProperties {

    private Logging logging = new Logging();

    public Logging getLogging() { return logging; }
    public void setLogging(Logging logging) { this.logging = logging == null ? new Logging() : logging; }

    public static class Logging {
        private Format format = Format.JSON;
        private Masking masking = new Masking();
        private Map<String, String> defaults = new LinkedHashMap<>();
        private Map<String, String> levels = new LinkedHashMap<>();

        public Format getFormat() { return format; }
        public void setFormat(Format format) { this.format = format == null ? Format.DEFAULT : format; }
        public Masking getMasking() { return masking; }
        public void setMasking(Masking masking) { this.masking = masking == null ? new Masking() : masking; }
        public Map<String, String> getDefaults() { return defaults; }
        public void setDefaults(Map<String, String> defaults) { this.defaults = defaults == null ? new LinkedHashMap<>() : new LinkedHashMap<>(defaults); }
        public Map<String, String> getLevels() { return levels; }
        public void setLevels(Map<String, String> levels) { this.levels = levels == null ? new LinkedHashMap<>() : new LinkedHashMap<>(levels); }

        public enum Format {
            DEFAULT,
            JSON
        }
    }

    public static class Masking {
        private boolean enabled = true;

        public boolean isEnabled() { return enabled; }
        public void setEnabled(boolean enabled) { this.enabled = enabled; }
    }
}
