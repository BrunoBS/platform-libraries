package br.com.portalmanager.platform.library.schemavalidation.repository;

import br.com.portalmanager.platform.library.schemavalidation.config.PlatformSchemaValidationProperties;
import br.com.portalmanager.platform.library.schemavalidation.model.ResourceSchema;
import org.springframework.jdbc.core.JdbcTemplate;

import java.util.Optional;

public class JdbcResourceSchemaRepository implements ResourceSchemaRepository {

    private final JdbcTemplate jdbcTemplate;
    private final PlatformSchemaValidationProperties properties;

    public JdbcResourceSchemaRepository(
            JdbcTemplate jdbcTemplate,
            PlatformSchemaValidationProperties properties
    ) {
        this.jdbcTemplate = jdbcTemplate;
        this.properties = properties;
    }

    @Override
    public Optional<ResourceSchema> find(String resourceType, String resourceCode) {
        String viewName = properties.resolveViewName();

        String sql = """
                SELECT resource_type, resource_code, schema_version, definition
                FROM %s
                WHERE resource_type = ?
                  AND resource_code = ?
                LIMIT 1
                """.formatted(viewName);

        return Optional.ofNullable(jdbcTemplate.query(
                sql,
                rs -> {
                    if (!rs.next()) {
                        return null;
                    }
                    return new ResourceSchema(
                            rs.getString("resource_type"),
                            rs.getString("resource_code"),
                            rs.getInt("schema_version"),
                            rs.getString("definition")
                    );
                },
                resourceType,
                resourceCode
        ));
    }
}
