package br.com.portalmanager.platform.library.schemavalidation.repository;

import br.com.portalmanager.platform.library.schemavalidation.config.PlatformSchemaValidationProperties;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.embedded.EmbeddedDatabaseBuilder;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.jdbc.datasource.embedded.EmbeddedDatabaseType.H2;

class JdbcResourceSchemaRepositoryTest {

    @Test
    void readsPublishedSchemaViewContract() {
        var dataSource = new EmbeddedDatabaseBuilder().setType(H2).build();
        var jdbc = new JdbcTemplate(dataSource);
        jdbc.execute("""
                CREATE TABLE vw_platform_resource_schemas (
                    resource_type VARCHAR(100),
                    resource_code VARCHAR(100),
                    schema_version INT,
                    definition VARCHAR(1000)
                )
                """);
        jdbc.update("""
                INSERT INTO vw_platform_resource_schemas
                (resource_type, resource_code, schema_version, definition)
                VALUES (?, ?, ?, ?)
                """, "APPLICATION", "application", 1, "{\"type\":\"object\"}");

        var properties = new PlatformSchemaValidationProperties();
        properties.getDatasource().setViewName("vw_platform_resource_schemas");
        var repository = new JdbcResourceSchemaRepository(jdbc, properties);

        var schema = repository.find("APPLICATION", "application");

        assertThat(schema).isPresent();
        assertThat(schema.orElseThrow().schemaVersion()).isEqualTo(1);
        assertThat(schema.orElseThrow().definition()).contains("\"object\"");
    }
}
