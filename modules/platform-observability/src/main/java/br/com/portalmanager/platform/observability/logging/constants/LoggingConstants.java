package br.com.portalmanager.platform.observability.logging.constants;

public final class LoggingConstants {
    
    private LoggingConstants() {} // Previne instanciação

    public static final String APPENDER_NAME = "JSON_CONSOLE";
    public static final String PROPERTY_DEFAULTS_PREFIX = "platform.observability.logging.defaults";
    public static final String PROPERTY_CUSTOM_PREFIX = "platform.observability.logging.levels";
    public static final String INTERNAL_PROPERTIES_FILE = "platform-observability-defaults.properties";
    public static final String ENVIRONMENT_PROPERTY_SOURCE_NAME = "platformLoggingDefaults";

    // O template do JSON isolado do código de orquestração
    public static final String JSON_PATTERN_TEMPLATE = 
            "{\"timestamp\":\"%%d{yyyy-MM-dd'T'HH:mm:ss.SSSX,UTC}\"," +
            "\"level\":\"%%level\"," +
            "\"thread\":\"%%thread\"," +
            "\"logger\":\"%%logger\"," +
            "\"message\":\"%%message\"," +
            "\"service\":\"%s\"," +
            "\"version\":\"%s\"," +
            "\"host\":\"%s\"," +
            "\"context\":%%jsonMdc," +
            "\"error\":%%jsonError," +
            "\"exception\":%%jsonThrowable}%%n";
}
