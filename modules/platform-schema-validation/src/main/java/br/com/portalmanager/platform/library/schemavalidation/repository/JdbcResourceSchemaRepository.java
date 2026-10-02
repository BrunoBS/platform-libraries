package br.com.portalmanager.platform.library.schemavalidation.repository;

import br.com.portalmanager.platform.library.messaging.exception.PlatformConfigurationException;
import br.com.portalmanager.platform.library.schemavalidation.config.PlatformSchemaValidationProperties;
import br.com.portalmanager.platform.library.schemavalidation.message.SchemaValidationTechnicalErrors;
import br.com.portalmanager.platform.library.schemavalidation.model.ResourceSchema;
import org.springframework.dao.DataAccessException;
import org.springframework.dao.EmptyResultDataAccessException;
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

    public void validateSource() {
        String sql = """
                SELECT resource_type, resource_code, schema_version, definition
                FROM %s
                WHERE 1 = 0
                """.formatted(properties.resolveViewName());

        try {
            jdbcTemplate.query(sql, rs -> null);
        } catch (DataAccessException exception) {
            throw new PlatformConfigurationException(
                    SchemaValidationTechnicalErrors.schemaSourceUnavailable(
                            properties.resolveViewName()
                    ),
                    exception
            );
        }
    }

    @Override
    public Optional<ResourceSchema> find(String resourceType, String resourceCode) {
        String viewName = properties.resolveViewName();

        String sql = """
                SELECT resource_type, resource_code, schema_version, definition
                FROM %s
                WHERE resource_type = ?
                  AND resource_code = ?
                """.formatted(viewName);

        try {
            return Optional.ofNullable(jdbcTemplate.queryForObject(
                    sql,
                    (rs, rowNum) -> new ResourceSchema(
                            rs.getString("resource_type"),
                            rs.getString("resource_code"),
                            rs.getInt("schema_version"),
                            rs.getString("definition")
                    ),
                    resourceType,
                    resourceCode
            ));
        } catch (EmptyResultDataAccessException exception) {
            return Optional.empty();
        }
    }
}
