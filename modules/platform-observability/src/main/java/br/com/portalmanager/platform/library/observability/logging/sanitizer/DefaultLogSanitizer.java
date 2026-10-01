package br.com.portalmanager.platform.library.observability.logging.sanitizer;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class DefaultLogSanitizer implements LogSanitizer {

    public static final DefaultLogSanitizer INSTANCE = new DefaultLogSanitizer();

    private static final Pattern CPF = Pattern.compile("\\b\\d{3}[.-]?\\d{3}[.-]?\\d{3}[.-]?\\d{2}\\b");
    private static final Pattern CARD = Pattern.compile("\\b\\d{4}[ -]?\\d{4}[ -]?\\d{4}[ -]?\\d{4}\\b");
    private static final Pattern EMAIL = Pattern.compile("\\b[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}\\b");
    private static final Pattern SENSITIVE_JSON_FIELD = Pattern.compile(
            "(?i)(\\\"(?:password|passwd|pwd|token|access_token|refresh_token|authorization|secret|client_secret|api_key|apikey)\\\"\\s*:\\s*\\\")(.*?)(\\\")"
    );

    private DefaultLogSanitizer() {}

    @Override
    public String sanitize(String value) {
        if (value == null || value.isBlank()) {
            return value;
        }
        String sanitized = maskSensitiveJsonFields(value);
        sanitized = CPF.matcher(sanitized).replaceAll("***.***.***-**");
        sanitized = CARD.matcher(sanitized).replaceAll("****-****-****-****");
        return maskEmails(sanitized);
    }

    private String maskSensitiveJsonFields(String value) {
        return SENSITIVE_JSON_FIELD.matcher(value).replaceAll("$1***$3");
    }

    private String maskEmails(String value) {
        Matcher matcher = EMAIL.matcher(value);
        StringBuilder result = new StringBuilder();
        while (matcher.find()) {
            String email = matcher.group();
            int at = email.indexOf('@');
            String masked = at > 1 ? email.charAt(0) + "****" + email.substring(at) : "****@****.***";
            matcher.appendReplacement(result, Matcher.quoteReplacement(masked));
        }
        matcher.appendTail(result);
        return result.toString();
    }
}
