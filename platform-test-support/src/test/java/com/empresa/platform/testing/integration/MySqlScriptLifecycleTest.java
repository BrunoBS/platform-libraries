package com.empresa.platform.testing.integration;

import com.empresa.platform.testing.annotation.PlatformIntegrationTest;
import com.empresa.platform.testing.annotation.WithDatabaseScripts;
import com.empresa.platform.testing.annotation.WithMySql;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.SpringBootConfiguration;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ContextConfiguration;
import org.testcontainers.junit.jupiter.Testcontainers;

import static org.assertj.core.api.Assertions.assertThat;

@Testcontainers(disabledWithoutDocker = true)
@PlatformIntegrationTest
@WithMySql
@ContextConfiguration(classes = MySqlScriptLifecycleTest.TestApplication.class)
@WithDatabaseScripts(
        setup = "classpath:sql/mysql/create-catalog-view.sql",
        cleanup = "classpath:sql/mysql/drop-catalog-view.sql"
)
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class MySqlScriptLifecycleTest {

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Test
    @Order(1)
    void shouldExposeClassViewAndLeaveDataForCleaner() {
        jdbcTemplate.update(
                "INSERT INTO catalogs (name, active) VALUES (?, ?)",
                "Catálogo de integração",
                true
        );

        Integer activeCatalogs = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM vw_active_catalogs",
                Integer.class
        );

        assertThat(activeCatalogs).isEqualTo(1);
    }

    @Test
    @Order(2)
    void shouldCleanBaseTablesAndKeepClassViewAvailable() {
        Integer catalogs = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM catalogs",
                Integer.class
        );
        Integer activeCatalogs = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM vw_active_catalogs",
                Integer.class
        );

        assertThat(catalogs).isZero();
        assertThat(activeCatalogs).isZero();
    }

    @Test
    @Order(3)
    @WithDatabaseScripts(
            setup = "classpath:sql/mysql/create-method-view.sql",
            cleanup = "classpath:sql/mysql/drop-method-view.sql"
    )
    void shouldExecuteClassAndMethodScripts() {
        Integer methodCatalogs = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM vw_method_catalogs",
                Integer.class
        );

        assertThat(methodCatalogs).isEqualTo(1);
    }

    @Test
    @Order(4)
    void shouldExecuteMethodCleanupScript() {
        Integer methodViews = jdbcTemplate.queryForObject(
                """
                SELECT COUNT(*)
                  FROM INFORMATION_SCHEMA.VIEWS
                 WHERE TABLE_SCHEMA = DATABASE()
                   AND TABLE_NAME = 'vw_method_catalogs'
                """,
                Integer.class
        );

        assertThat(methodViews).isZero();
    }

    @SpringBootConfiguration
    @EnableAutoConfiguration
    static class TestApplication {
    }
}
