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
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Properties;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class PlatformDefaultMessageProvider implements ApiMessageProvider {

    private static final String RESOURCE_PATTERN =
            "classpath*:META-INF/platform-messages/*.properties";

    private static final Pattern BUNDLE_FILENAME_PATTERN =
            Pattern.compile("^.+_([a-z]{2}(?:_[A-Z]{2})?)\\.properties$");

    private final ApiMessageDefinitionParser parser;
    private final Map<String, Map<String, String>> definitionsByLocale;

    public PlatformDefaultMessageProvider() {
        this(new PathMatchingResourcePatternResolver(), new ApiMessageDefinitionParser());
    }

    PlatformDefaultMessageProvider(
            PathMatchingResourcePatternResolver resourceResolver,
            ApiMessageDefinitionParser parser
    ) {
        this.parser = parser;
        this.definitionsByLocale = loadAndValidate(resourceResolver);
    }

    @Override
    public Optional<ApiMessage> find(String key, Locale locale) {
        if (key == null || key.isBlank() || locale == null || Locale.ROOT.equals(locale)) {
            return Optional.empty();
        }

        Map<String, String> definitions = definitionsByLocale.get(locale.toString());
        if (definitions == null) {
            return Optional.empty();
        }

        String definition = definitions.get(key);
        if (definition == null || definition.isBlank()) {
            return Optional.empty();
        }

        return Optional.of(parser.parse(key, locale, definition));
    }

    private Map<String, Map<String, String>> loadAndValidate(
            PathMatchingResourcePatternResolver resourceResolver
    ) {
        try {
            Resource[] resources = resourceResolver.getResources(RESOURCE_PATTERN);
            Arrays.sort(resources, Comparator.comparing(this::resourceName));

            Map<String, Map<String, String>> definitions = new HashMap<>();
            Map<String, Map<String, String>> owners = new HashMap<>();

            for (Resource resource : resources) {
                String locale = extractLocale(resource);
                if (locale == null) {
                    continue;
                }

                Properties properties = loadProperties(resource);
                Map<String, String> localeDefinitions =
                        definitions.computeIfAbsent(locale, ignored -> new HashMap<>());
                Map<String, String> localeOwners =
                        owners.computeIfAbsent(locale, ignored -> new HashMap<>());

                for (String key : properties.stringPropertyNames()) {
                    String currentOwner = localeOwners.putIfAbsent(key, resourceName(resource));
                    if (currentOwner != null) {
                        throw duplicateKeyException(
                                key,
                                locale,
                                currentOwner,
                                resourceName(resource)
                        );
                    }

                    localeDefinitions.put(key, properties.getProperty(key));
                }
            }

            Map<String, Map<String, String>> immutableDefinitions = new HashMap<>();
            definitions.forEach((locale, values) ->
                    immutableDefinitions.put(locale, Map.copyOf(values)));

            return Map.copyOf(immutableDefinitions);
        } catch (IOException exception) {
            throw new IllegalStateException(
                    "Could not load platform message bundles from " + RESOURCE_PATTERN,
                    exception
            );
        }
    }

    private Properties loadProperties(Resource resource) throws IOException {
        return PropertiesLoaderUtils.loadProperties(
                new EncodedResource(resource, StandardCharsets.UTF_8)
        );
    }

    private String extractLocale(Resource resource) {
        String filename = resource.getFilename();
        if (filename == null) {
            return null;
        }

        Matcher matcher = BUNDLE_FILENAME_PATTERN.matcher(filename);
        return matcher.matches() ? matcher.group(1) : null;
    }

    private IllegalStateException duplicateKeyException(
            String key,
            String locale,
            String firstResource,
            String secondResource
    ) {
        return new IllegalStateException(
                "Duplicate platform message key detected: key='%s', locale='%s', bundles=['%s', '%s']"
                        .formatted(key, locale, firstResource, secondResource)
        );
    }

    private String resourceName(Resource resource) {
        String filename = resource.getFilename();
        return filename == null ? resource.getDescription() : filename;
    }
}
