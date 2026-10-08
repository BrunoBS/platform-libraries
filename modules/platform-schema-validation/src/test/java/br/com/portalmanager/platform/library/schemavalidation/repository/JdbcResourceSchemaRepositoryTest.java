package br.com.portalmanager.platform.library.schemavalidation.repository;

import br.com.portalmanager.platform.library.messaging.exception.PlatformConfigurationException;
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

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowableOfType;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
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
    void shouldValidateDefaultViewContractWithoutReadingBusinessData() {
        repository.validateSource();

        verify(jdbcTemplate).query(
                org.mockito.ArgumentMatchers.<String>argThat(sql ->
                        sql.contains("FROM vw_platform_resource_schemas")
                                && sql.contains("WHERE 1 = 0")
                                && sql.contains("resource_type")
                                && sql.contains("resource_code")
                                && sql.contains("schema_version")
                                && sql.contains("definition")
                ),
                org.mockito.ArgumentMatchers.<org.springframework.jdbc.core.ResultSetExtractor<Object>>any()
        );
    }

    @Test
    void shouldFailSourceValidationWhenViewIsUnavailable() {
        when(jdbcTemplate.query(
                any(String.class),
                org.mockito.ArgumentMatchers.<org.springframework.jdbc.core.ResultSetExtractor<Object>>any()
        )).thenThrow(new DataAccessResourceFailureException("view unavailable"));

        PlatformConfigurationException exception = catchThrowableOfType(
                repository::validateSource,
                PlatformConfigurationException.class
        );
        assertThat(exception).isNotNull();

        assertThat(exception.getCause() instanceof DataAccessResourceFailureException)
                .isTrue();
        assertThat(exception.getErrorResponse().code())
                .isEqualTo("PLT-SCHEMA-005");
        assertThat(exception.getErrorResponse().message().contains("vw_platform_resource_schemas"))
                .isTrue();
        assertThat(exception.getErrorResponse().solution().contains("platform.schema-validation.view-name"))
                .isTrue();
        assertThat(exception.getErrorResponse().solution().contains("ResourceSchemaRepository"))
                .isTrue();
        assertThat(exception.getMessage().contains("resource_type, resource_code, schema_version, definition"))
                .isTrue();
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

        assertThat(result.isPresent())
                .isTrue();
        assertThat(result.orElseThrow())
                .isEqualTo(expected);
    }

    @Test
    void shouldReturnEmptyWhenSchemaIsNotFoundInView() {
        when(jdbcTemplate.queryForObject(
                any(String.class),
                any(RowMapper.class),
                eq("MENU"),
                eq("menu")
        )).thenThrow(new EmptyResultDataAccessException(1));

        assertThat(repository.find("MENU", "menu").isEmpty())
                .isTrue();
    }

    @Test
    void shouldPropagateDuplicateRowsAsInvalidViewContract() {
        when(jdbcTemplate.queryForObject(
                any(String.class),
                any(RowMapper.class),
                eq("APPLICATION"),
                eq("application")
        )).thenThrow(new IncorrectResultSizeDataAccessException(1, 2));

        PlatformConfigurationException exception = catchThrowableOfType(
                () -> repository.find("APPLICATION", "application"),
                PlatformConfigurationException.class
        );
        assertThat(exception).isNotNull();

        assertThat(exception.getCause() instanceof IncorrectResultSizeDataAccessException)
                .isTrue();
    }

    @Test
    void shouldPropagateDatasourceFailureInsteadOfTreatingItAsMissingSchema() {
        when(jdbcTemplate.queryForObject(
                any(String.class),
                any(RowMapper.class),
                eq("APPLICATION"),
                eq("application")
        )).thenThrow(new DataAccessResourceFailureException("database unavailable"));

        PlatformConfigurationException exception = catchThrowableOfType(
                () -> repository.find("APPLICATION", "application"),
                PlatformConfigurationException.class
        );
        assertThat(exception).isNotNull();

        assertThat(exception.getCause() instanceof DataAccessResourceFailureException)
                .isTrue();
    }
}
