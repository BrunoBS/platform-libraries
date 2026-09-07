package com.empresa.platform.messaging.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.time.Duration;

@ConfigurationProperties(prefix = "platform.messaging")
public class PlatformMessagingProperties {
    private boolean enabled = true;
    private String mdcCorrelationKey = "traceId";
    private String defaultLocale = "pt-BR";
    private final Datasource datasource = new Datasource();
    private final Cache cache = new Cache();

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean v) {
        enabled = v;
    }

    public String getMdcCorrelationKey() {
        return mdcCorrelationKey;
    }

    public void setMdcCorrelationKey(String mdcCorrelationKey) {
        this.mdcCorrelationKey = mdcCorrelationKey;
    }

    public String getDefaultLocale() {
        return defaultLocale;
    }

    public void setDefaultLocale(String v) {
        defaultLocale = v;
    }

    public Datasource getDatasource() {
        return datasource;
    }

    public Cache getCache() {
        return cache;
    }

    public static class Datasource {
        private String viewName = "vw_api_message";

        public String getViewName() {
            return viewName;
        }

        public void setViewName(String v) {
            viewName = v;
        }
    }

    public static class Cache {
        private boolean enabled = false;
        private Duration ttl = Duration.ofHours(1);

        public boolean isEnabled() {
            return enabled;
        }

        public void setEnabled(boolean v) {
            enabled = v;
        }

        public Duration getTtl() {
            return ttl;
        }

        public void setTtl(Duration v) {
            ttl = v;
        }
    }
}
