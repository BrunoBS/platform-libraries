package br.com.portalmanager.platform.library.schemavalidation.resolver;

import br.com.portalmanager.platform.library.schemavalidation.config.PlatformSchemaValidationProperties;
import br.com.portalmanager.platform.library.schemavalidation.cache.ResourceSchemaCache;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import br.com.portalmanager.platform.library.schemavalidation.model.ResourceSchema;
import br.com.portalmanager.platform.library.schemavalidation.repository.ResourceSchemaRepository;
import br.com.portalmanager.platform.library.schemavalidation.message.SchemaValidationTechnicalErrors;
import br.com.portalmanager.platform.library.messaging.exception.PlatformConfigurationException;

public class DefaultResourceSchemaResolver implements ResourceSchemaResolver {

    private static final Logger log = LoggerFactory.getLogger(DefaultResourceSchemaResolver.class);

    private final ResourceSchemaRepository repository;
    private final PlatformSchemaValidationProperties properties;
    private final ResourceSchemaCache cache;

    public DefaultResourceSchemaResolver(ResourceSchemaRepository repository, PlatformSchemaValidationProperties properties) {
        this(repository, properties, new br.com.portalmanager.platform.library.schemavalidation.cache.NoOpResourceSchemaCache());
    }

    public DefaultResourceSchemaResolver(
            ResourceSchemaRepository repository,
            PlatformSchemaValidationProperties properties,
            ResourceSchemaCache cache
    ) {
        this.repository = repository;
        this.properties = properties;
        this.cache = cache;
    }

    @Override
    public ResourceSchema resolve(String resourceType, String resourceCode) {
        String type = requireText(resourceType, "resourceType");
        String code = requireText(resourceCode, "resourceCode");

        ResourceSchema exact = find(type, code).orElse(null);
        if (exact != null) {
            return exact;
        }

        return find(type, properties.resolveFallbackCode())
                .orElseThrow(() -> new PlatformConfigurationException(
                        SchemaValidationTechnicalErrors.publishedSchemaNotFound(type, code)
                ));
    }

    private java.util.Optional<ResourceSchema> find(String type, String code) {
        try {
            var cached = cache.get(type, code);
            if (cached.isPresent()) {
                return cached;
            }
        } catch (RuntimeException exception) {
            log.error("Schema validation Redis cache unavailable; continuing with datasource", exception);
        }

        var resolved = repository.find(type, code);
        resolved.ifPresent(schema -> {
            try {
                cache.put(schema);
            } catch (RuntimeException exception) {
                log.error("Schema validation Redis cache unavailable while storing schema; continuing", exception);
            }
        });
        return resolved;
    }

    private String requireText(String value, String field) {
        if (value == null || value.isBlank()) {
            throw new PlatformConfigurationException(
                    SchemaValidationTechnicalErrors.requiredResolverArgument(field)
            );
        }
        return value.trim();
    }
}
