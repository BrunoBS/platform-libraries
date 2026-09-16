package com.empresa.platform.messaging.message;

import com.empresa.platform.messaging.model.ApiMessage;
import com.empresa.platform.messaging.provider.ApiMessageProvider;
import org.springframework.context.support.ResourceBundleMessageSource;
import org.springframework.core.io.ClassPathResource;

import java.nio.charset.StandardCharsets;
import java.util.Locale;
import java.util.Optional;

public final class PlatformDefaultMessageProvider implements ApiMessageProvider {

    private static final String BASENAME = "messages/platform-messages";
    private static final String RESOURCE_BASENAME = "messages/platform-messages";

    private final ResourceBundleMessageSource messageSource;
    private final ApiMessageDefinitionParser parser;

    public PlatformDefaultMessageProvider() {
        this(createMessageSource(), new ApiMessageDefinitionParser());
    }

    PlatformDefaultMessageProvider(
            ResourceBundleMessageSource messageSource,
            ApiMessageDefinitionParser parser
    ) {
        this.messageSource = messageSource;
        this.parser = parser;
    }

    @Override
    public Optional<ApiMessage> find(String key, Locale locale) {
        if (key == null || key.isBlank() || locale == null || Locale.ROOT.equals(locale)) {
            return Optional.empty();
        }

        if (!hasExactBundle(locale)) {
            return Optional.empty();
        }

        String rawDefinition = messageSource.getMessage(key, null, null, locale);
        if (rawDefinition == null || rawDefinition.isBlank()) {
            return Optional.empty();
        }

        return Optional.of(parser.parse(key, locale, rawDefinition));
    }

    private boolean hasExactBundle(Locale locale) {
        String suffix = locale.toString();
        if (suffix.isBlank()) {
            return false;
        }

        return new ClassPathResource(
                RESOURCE_BASENAME + "_" + suffix + ".properties"
        ).exists();
    }

    private static ResourceBundleMessageSource createMessageSource() {
        ResourceBundleMessageSource source = new ResourceBundleMessageSource();
        source.setBasename(BASENAME);
        source.setDefaultEncoding(StandardCharsets.UTF_8.name());
        source.setFallbackToSystemLocale(false);
        return source;
    }
}
