package com.empresa.platform.messaging.message;

import com.empresa.platform.messaging.model.ApiMessage;
import com.empresa.platform.messaging.provider.ApiMessageProvider;
import org.springframework.core.io.Resource;
import org.springframework.core.io.support.EncodedResource;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;
import org.springframework.core.io.support.PropertiesLoaderUtils;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Comparator;
import java.util.Locale;
import java.util.Optional;
import java.util.Properties;

public final class PlatformDefaultMessageProvider implements ApiMessageProvider {

    private static final String RESOURCE_PATTERN =
            "classpath*:META-INF/platform-messages/*_%s.properties";

    private final PathMatchingResourcePatternResolver resourceResolver;
    private final ApiMessageDefinitionParser parser;

    public PlatformDefaultMessageProvider() {
        this(new PathMatchingResourcePatternResolver(), new ApiMessageDefinitionParser());
    }

    PlatformDefaultMessageProvider(
            PathMatchingResourcePatternResolver resourceResolver,
            ApiMessageDefinitionParser parser
    ) {
        this.resourceResolver = resourceResolver;
        this.parser = parser;
    }

    @Override
    public Optional<ApiMessage> find(String key, Locale locale) {
        if (key == null || key.isBlank() || locale == null || Locale.ROOT.equals(locale)) {
            return Optional.empty();
        }

        String suffix = locale.toString();
        if (suffix.isBlank()) {
            return Optional.empty();
        }

        try {
            Resource[] resources = resourceResolver.getResources(
                    RESOURCE_PATTERN.formatted(suffix)
            );

            return Arrays.stream(resources)
                    .sorted(Comparator.comparing(this::resourceName))
                    .map(resource -> findInResource(resource, key))
                    .flatMap(Optional::stream)
                    .findFirst()
                    .map(definition -> parser.parse(key, locale, definition));
        } catch (IOException exception) {
            return Optional.empty();
        }
    }

    private Optional<String> findInResource(Resource resource, String key) {
        try {
            Properties properties = PropertiesLoaderUtils.loadProperties(
                    new EncodedResource(resource, StandardCharsets.UTF_8)
            );
            String definition = properties.getProperty(key);
            return definition == null || definition.isBlank()
                    ? Optional.empty()
                    : Optional.of(definition);
        } catch (IOException exception) {
            return Optional.empty();
        }
    }

    private String resourceName(Resource resource) {
        String filename = resource.getFilename();
        return filename == null ? resource.getDescription() : filename;
    }
}
