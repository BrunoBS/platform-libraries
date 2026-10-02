package br.com.portalmanager.platform.library.schemavalidation.repository;

import br.com.portalmanager.platform.library.schemavalidation.config.PlatformSchemaValidationProperties;
import br.com.portalmanager.platform.library.schemavalidation.model.ResourceSchema;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.ResultSetExtractor;
import org.springframework.dao.DataAccessResourceFailureException;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class JdbcResourceSchemaRepositoryTest {

    private JdbcTemplate jdbcTemplate;
    private JdbcResourceSchemaRepository repository;

    @BeforeEach
    void setUp() {
        jdbcTemplate = mock(JdbcTemplate.class);

        PlatformSchemaValidationProperties properties = new PlatformSchemaValidationProperties();
        properties.setViewName("vw_platform_resource_schemas");

        repository = new JdbcResourceSchemaRepository(jdbcTemplate, properties);
    }

    @Test
    void shouldReturnResourceSchemaWhenFoundInView() throws SQLException {
        ResultSet resultSet = mock(ResultSet.class);
        when(resultSet.next()).thenReturn(true);
        when(resultSet.getString("resource_type")).thenReturn("APPLICATION");
        when(resultSet.getString("resource_code")).thenReturn("application");
        when(resultSet.getInt("schema_version")).thenReturn(1);
        when(resultSet.getString("definition")).thenReturn("{\"type\":\"object\"}");

        when(jdbcTemplate.query(
                any(String.class),
                any(ResultSetExtractor.class),
                eq("APPLICATION"),
                eq("application")
        )).thenAnswer(invocation -> {
            ResultSetExtractor<ResourceSchema> extractor = invocation.getArgument(1);
            return extractor.extractData(resultSet);
        });

        Optional<ResourceSchema> result = repository.find("APPLICATION", "application");

        assertTrue(result.isPresent());
        ResourceSchema schema = result.orElseThrow();
        assertEquals("APPLICATION", schema.resourceType());
        assertEquals("application", schema.resourceCode());
        assertEquals(1, schema.schemaVersion());
        assertEquals("{\"type\":\"object\"}", schema.definition());
    }

    @Test
    void shouldPropagateDatasourceFailureInsteadOfTreatingItAsMissingSchema() {
        when(jdbcTemplate.query(
                any(String.class),
                any(ResultSetExtractor.class),
                eq("APPLICATION"),
                eq("application")
        )).thenThrow(new DataAccessResourceFailureException("database unavailable"));

        assertThrows(
                DataAccessResourceFailureException.class,
                () -> repository.find("APPLICATION", "application")
        );
    }

    @Test
    void shouldReturnEmptyWhenSchemaIsNotFoundInView() throws SQLException {
        ResultSet resultSet = mock(ResultSet.class);
        when(resultSet.next()).thenReturn(false);

        when(jdbcTemplate.query(
                any(String.class),
                any(ResultSetExtractor.class),
                eq("MENU"),
                eq("menu")
        )).thenAnswer(invocation -> {
            ResultSetExtractor<ResourceSchema> extractor = invocation.getArgument(1);
            return extractor.extractData(resultSet);
        });

        Optional<ResourceSchema> result = repository.find("MENU", "menu");

        assertTrue(result.isEmpty());
    }
}
