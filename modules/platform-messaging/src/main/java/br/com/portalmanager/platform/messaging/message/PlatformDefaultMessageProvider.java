package br.com.portalmanager.platform.messaging.message;

import br.com.portalmanager.platform.messaging.model.ApiMessage;
import br.com.portalmanager.platform.messaging.provider.ApiMessageProvider;
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

    private static final String RESOURCE_DIRECTORY = "META-INF/platform-messages";
    private static final String RESOURCE_PATTERN =
            "classpath*:" + RESOURCE_DIRECTORY + "/*.properties";

    private static final Pattern BUNDLE_FILENAME_PATTERN =
            Pattern.compile("^([a-z0-9-]+)_([a-z]{2}(?:_[A-Z]{2})?)\\.properties$");

    private final PathMatchingResourcePatternResolver resourceResolver;
    private final ApiMessageDefinitionParser parser;
    private final Map<String, Map<String, String>> definitionsByLocale;

    public PlatformDefaultMessageProvider() {
        this(new PathMatchingResourcePatternResolver(), new ApiMessageDefinitionParser());
    }

    PlatformDefaultMessageProvider(
            PathMatchingResourcePatternResolver resourceResolver,
            ApiMessageDefinitionParser parser
    ) {
        this.resourceResolver = resourceResolver;
        this.parser = parser;
        this.definitionsByLocale = loadAndValidate(resourceResolver);
    }

    @Override
    public Optional<ApiMessage> find(String key, Locale locale) {
        if (key == null || key.isBlank() || locale == null || Locale.ROOT.equals(locale)) {
            return Optional.empty();
        }

        Map<String, String> definitions = definitionsByLocale.get(locale.toString());
        if (definitions != null) {
            String definition = definitions.get(key);
            if (definition != null && !definition.isBlank()) {
                return Optional.of(parser.parse(key, locale, definition));
            }
        }

        return findDirectlyInServiceBundle(key, locale);
    }

    private Optional<ApiMessage> findDirectlyInServiceBundle(String key, Locale locale) {
        int namespaceSeparator = key.indexOf('.');
        if (namespaceSeparator <= 0 || namespaceSeparator == key.length() - 1) {
            return Optional.empty();
        }

        String service = key.substring(0, namespaceSeparator);
        String localKey = key.substring(namespaceSeparator + 1);
        String resourceLocation = "classpath*:%s/%s_%s.properties"
                .formatted(RESOURCE_DIRECTORY, service, locale);

        try {
            Resource[] resources = resourceResolver.getResources(resourceLocation);
            ApiMessage resolved = null;
            String owner = null;

            for (Resource resource : resources) {
                Properties properties = loadProperties(resource);
                String definition = properties.getProperty(localKey);
                if (definition == null || definition.isBlank()) {
                    continue;
                }

                if (resolved != null) {
                    throw duplicateKeyException(
                            key,
                            locale.toString(),
                            owner,
                            resourceName(resource)
                    );
                }

                resolved = parser.parse(key, locale, definition);
                owner = resourceName(resource);
            }

            return Optional.ofNullable(resolved);
        } catch (IOException exception) {
            throw new IllegalStateException(
                    "Could not load service message bundle from " + resourceLocation,
                    exception
            );
        }
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
                BundleDescriptor bundle = extractBundleDescriptor(resource);
                if (bundle == null) {
                    throw new IllegalStateException(
                            "Invalid platform message bundle name: '%s'. Expected: <service>_<locale>.properties"
                                    .formatted(resourceName(resource))
                    );
                }

                Properties properties = loadProperties(resource);
                Map<String, String> localeDefinitions =
                        definitions.computeIfAbsent(bundle.locale(), ignored -> new HashMap<>());
                Map<String, String> localeOwners =
                        owners.computeIfAbsent(bundle.locale(), ignored -> new HashMap<>());

                for (String localKey : properties.stringPropertyNames()) {
                    validateLocalKey(bundle.service(), localKey, resource);

                    String globalKey = bundle.service() + "." + localKey;
                    String currentOwner = localeOwners.putIfAbsent(globalKey, resourceName(resource));
                    if (currentOwner != null) {
                        throw duplicateKeyException(
                                globalKey,
                                bundle.locale(),
                                currentOwner,
                                resourceName(resource)
                        );
                    }

                    localeDefinitions.put(globalKey, properties.getProperty(localKey));
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

    private void validateLocalKey(String service, String localKey, Resource resource) {
        if (localKey == null || localKey.isBlank()) {
            throw new IllegalStateException(
                    "Blank platform message key in bundle '%s'".formatted(resourceName(resource))
            );
        }

        if (localKey.startsWith(service + ".")) {
            throw new IllegalStateException(
                    ("Platform message key must be local to its bundle: key='%s', bundle='%s'. "
                            + "Remove the '%s.' prefix.")
                            .formatted(localKey, resourceName(resource), service)
            );
        }
    }

    private Properties loadProperties(Resource resource) throws IOException {
        return PropertiesLoaderUtils.loadProperties(
                new EncodedResource(resource, StandardCharsets.UTF_8)
        );
    }

    private BundleDescriptor extractBundleDescriptor(Resource resource) {
        String filename = resource.getFilename();
        if (filename == null) {
            return null;
        }

        Matcher matcher = BUNDLE_FILENAME_PATTERN.matcher(filename);
        if (!matcher.matches()) {
            return null;
        }

        return new BundleDescriptor(matcher.group(1), matcher.group(2));
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

    private record BundleDescriptor(String service, String locale) {
    }
}
