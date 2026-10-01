package br.com.portalmanager.platform.library.messaging.message;

import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class MessageParameterResolver {

    private static final Pattern PARAMETER_PATTERN = Pattern.compile("\\{([^}]+)}");

    private MessageParameterResolver() {
    }

    public static String resolve(String text, Map<String, Object> params) {
        if (text == null || text.isBlank() || params == null || params.isEmpty()) {
            return text;
        }

        Matcher matcher = PARAMETER_PATTERN.matcher(text);
        StringBuilder resultBuilder = new StringBuilder();

        while (matcher.find()) {
            String parameterKey = matcher.group(1);
            Object paramValue = params.get(parameterKey);

            String replacement = (paramValue == null) ? matcher.group(0) : String.valueOf(paramValue);

            matcher.appendReplacement(resultBuilder, Matcher.quoteReplacement(replacement));
        }

        matcher.appendTail(resultBuilder);
        return resultBuilder.toString();
    }
}
