package br.com.portalmanager.platform.library.testing.integration;

import br.com.portalmanager.platform.library.testing.lifecycle.annotation.PlatformIntegrationTest;
import br.com.portalmanager.platform.library.testing.database.annotation.WithDatabaseScripts;
import br.com.portalmanager.platform.library.testing.database.annotation.WithMySql;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.SpringBootConfiguration;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.TestPropertySource;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.testcontainers.containers.MySQLContainer;
import org.testcontainers.junit.jupiter.Testcontainers;

import static org.assertj.core.api.Assertions.assertThat;

@Testcontainers(disabledWithoutDocker = true)
@PlatformIntegrationTest
@WithMySql
@ContextConfiguration(classes = MySqlScriptLifecycleIT.TestApplication.class)
@TestPropertySource(properties = {
        "platform.authorization.enabled=false",
        "platform.messaging.enabled=false"
})
@WithDatabaseScripts(
        setup = "classpath:sql/mysql/create-catalog-view.sql",
        cleanup = "classpath:sql/mysql/drop-catalog-view.sql"
)
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class MySqlScriptLifecycleIT {

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private MySQLContainer<?> mySqlContainer;

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

    @Test
    void shouldShareContainerAcrossIndependentSpringContextsAndKeepItRunningOnContextClose() {
        MySQLContainer<?> firstContextContainer = createIndependentContextContainer();
        MySQLContainer<?> secondContextContainer = createIndependentContextContainer();

        assertThat(firstContextContainer).isSameAs(mySqlContainer);
        assertThat(secondContextContainer).isSameAs(mySqlContainer);
        assertThat(mySqlContainer.isRunning()).isTrue();
    }

    private MySQLContainer<?> createIndependentContextContainer() {
        AnnotationConfigApplicationContext context = new AnnotationConfigApplicationContext();
        context.register(br.com.portalmanager.platform.library.testing.database.MySqlTestConfiguration.class);
        context.refresh();
        MySQLContainer<?> container = context.getBean(MySQLContainer.class);
        context.close();
        return container;
    }

    @SpringBootConfiguration
    @EnableAutoConfiguration
    static class TestApplication {
    }
}
