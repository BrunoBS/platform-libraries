package br.com.portalmanager.platform.library.schemavalidation.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "platform.schema-validation")
public class PlatformSchemaValidationProperties {

    public static final String DEFAULT_FALLBACK_CODE = "DEFAULT";
    public static final String DEFAULT_VIEW_NAME = "vw_platform_resource_schemas";

    private boolean enabled = true;
    private String fallbackCode = DEFAULT_FALLBACK_CODE;
    private final Datasource datasource = new Datasource();

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public String getFallbackCode() {
        return fallbackCode;
    }

    public void setFallbackCode(String fallbackCode) {
        this.fallbackCode = fallbackCode;
    }

    public String resolveFallbackCode() {
        return fallbackCode == null || fallbackCode.isBlank()
                ? DEFAULT_FALLBACK_CODE
                : fallbackCode.trim();
    }

    public Datasource getDatasource() {
        return datasource;
    }

    public static class Datasource {
        private boolean enabled = false;
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

        public String resolveViewName() {
            return viewName == null || viewName.isBlank()
                    ? DEFAULT_VIEW_NAME
                    : viewName.trim();
        }
    }
}
