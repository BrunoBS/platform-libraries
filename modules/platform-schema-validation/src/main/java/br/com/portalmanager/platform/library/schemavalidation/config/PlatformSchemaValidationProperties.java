package br.com.portalmanager.platform.library.schemavalidation.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "platform.schema-validation")
public class PlatformSchemaValidationProperties {

    public static final String DEFAULT_FALLBACK_CODE = "DEFAULT";
    public static final String DEFAULT_VIEW_NAME = "vw_platform_resource_schemas";

    private String fallbackCode = DEFAULT_FALLBACK_CODE;
    private String viewName = DEFAULT_VIEW_NAME;

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
