package br.com.portalmanager.platform.library.schemavalidation.message;

public final class SchemaValidationMessageKeys {

    private static final String PREFIX = "schemavalidation.";

    public static final String INVALID = PREFIX + "invalid";
    public static final String REQUIRED = PREFIX + "required";
    public static final String TYPE = PREFIX + "type";
    public static final String MIN_LENGTH = PREFIX + "min-length";
    public static final String MAX_LENGTH = PREFIX + "max-length";
    public static final String MINIMUM = PREFIX + "minimum";
    public static final String MAXIMUM = PREFIX + "maximum";
    public static final String PATTERN = PREFIX + "pattern";
    public static final String ENUM = PREFIX + "enum";
    public static final String ADDITIONAL_PROPERTIES = PREFIX + "additional-properties";

    private SchemaValidationMessageKeys() {
    }
}
