package br.com.portalmanager.platform.library.schemavalidation.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "platform.schema-validation")
public class PlatformSchemaValidationProperties {

    private boolean enabled = true;
    private String fallbackCode = "DEFAULT";
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

    public Datasource getDatasource() {
        return datasource;
    }

    public static class Datasource {
        private boolean enabled = false;
        private String viewName = "vw_platform_resource_schemas";

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
}
