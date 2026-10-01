package br.com.portalmanager.platform.library.messaging.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.time.Duration;
import java.util.Locale;

@ConfigurationProperties(prefix = "platform.messaging")
public class PlatformMessagingProperties {

    public static final String DEFAULT_LOCALE = "pt-BR";
    public static final String DEFAULT_VIEW_NAME = "vw_api_message";
    public static final Duration DEFAULT_CACHE_TTL = Duration.ofHours(1);

    private boolean enabled = true;
    private String defaultLocale = DEFAULT_LOCALE;
    private final Datasource datasource = new Datasource();
    private final Cache cache = new Cache();

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public String getDefaultLocale() {
        return defaultLocale;
    }

    public void setDefaultLocale(String defaultLocale) {
        this.defaultLocale = defaultLocale;
    }

    public Locale resolveDefaultLocale() {
        if (defaultLocale == null || defaultLocale.isBlank()) {
            return Locale.forLanguageTag(DEFAULT_LOCALE);
        }

        Locale locale = Locale.forLanguageTag(defaultLocale.trim());
        return Locale.ROOT.equals(locale)
                ? Locale.forLanguageTag(DEFAULT_LOCALE)
                : locale;
    }

    public Datasource getDatasource() {
        return datasource;
    }

    public Cache getCache() {
        return cache;
    }

    public static class Datasource {
        private boolean enabled;
        private String viewName = DEFAULT_VIEW_NAME;

        public boolean isEnabled() {
            return enabled;
        }

        public void setEnabled(boolean enabled) {
            this.enabled = enabled;
        }

        public String getViewName() {
            return viewName;
        }

        public void setViewName(String viewName) {
            this.viewName = viewName;
        }
    }

    public static class Cache {
        private boolean enabled;
        private Duration ttl = DEFAULT_CACHE_TTL;

        public boolean isEnabled() {
            return enabled;
        }

        public void setEnabled(boolean enabled) {
            this.enabled = enabled;
        }

        public Duration getTtl() {
            return ttl;
        }

        public void setTtl(Duration ttl) {
            this.ttl = ttl;
        }
    }
}
