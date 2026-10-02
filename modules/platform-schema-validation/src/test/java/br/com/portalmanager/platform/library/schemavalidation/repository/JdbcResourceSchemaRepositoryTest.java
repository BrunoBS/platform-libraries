package br.com.portalmanager.platform.library.schemavalidation.repository;

import br.com.portalmanager.platform.library.schemavalidation.config.PlatformSchemaValidationProperties;
import br.com.portalmanager.platform.library.schemavalidation.model.ResourceSchema;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.dao.DataAccessResourceFailureException;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.dao.IncorrectResultSizeDataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
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
    void shouldReturnResourceSchemaWhenFoundInView() {
        ResourceSchema expected = new ResourceSchema(
                "APPLICATION",
                "application",
                1,
                "{\"type\":\"object\"}"
        );

        when(jdbcTemplate.queryForObject(
                any(String.class),
                any(RowMapper.class),
                eq("APPLICATION"),
                eq("application")
        )).thenReturn(expected);

        Optional<ResourceSchema> result = repository.find("APPLICATION", "application");

        assertTrue(result.isPresent());
        assertEquals(expected, result.orElseThrow());
    }

    @Test
    void shouldReturnEmptyWhenSchemaIsNotFoundInView() {
        when(jdbcTemplate.queryForObject(
                any(String.class),
                any(RowMapper.class),
                eq("MENU"),
                eq("menu")
        )).thenThrow(new EmptyResultDataAccessException(1));

        assertTrue(repository.find("MENU", "menu").isEmpty());
    }

    @Test
    void shouldPropagateDuplicateRowsAsInvalidViewContract() {
        when(jdbcTemplate.queryForObject(
                any(String.class),
                any(RowMapper.class),
                eq("APPLICATION"),
                eq("application")
        )).thenThrow(new IncorrectResultSizeDataAccessException(1, 2));

        assertThrows(
                IncorrectResultSizeDataAccessException.class,
                () -> repository.find("APPLICATION", "application")
        );
    }

    @Test
    void shouldPropagateDatasourceFailureInsteadOfTreatingItAsMissingSchema() {
        when(jdbcTemplate.queryForObject(
                any(String.class),
                any(RowMapper.class),
                eq("APPLICATION"),
                eq("application")
        )).thenThrow(new DataAccessResourceFailureException("database unavailable"));

        assertThrows(
                DataAccessResourceFailureException.class,
                () -> repository.find("APPLICATION", "application")
        );
    }
}
