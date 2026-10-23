package br.com.portalmanager.platform.library.testing.database;

import br.com.portalmanager.platform.library.testing.database.annotation.WithMySql;
import br.com.portalmanager.platform.library.testing.lifecycle.annotation.PlatformIntegrationTest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.SpringBootConfiguration;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.TestPropertySource;
import org.testcontainers.junit.jupiter.Testcontainers;

import static org.assertj.core.api.Assertions.assertThat;

@Testcontainers(disabledWithoutDocker = true)
@PlatformIntegrationTest
@WithMySql(cleanup = CleanupMode.NONE)
@ContextConfiguration(classes = DatabaseCleanerBatchIT.TestApplication.class)
@TestPropertySource(properties = {
        "platform.authorization.enabled=false",
        "platform.messaging.enabled=false"
})
class DatabaseCleanerBatchIT {

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private MySqlCleanerDataSource cleanerDataSource;

    @Test
    void batchesTruncatesAndHonorsExcludedTables() {
        jdbcTemplate.execute("""
                CREATE TABLE cleanup_batch_target (
                    id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
                    value VARCHAR(30) NOT NULL
                )
                """);
        jdbcTemplate.execute("""
                CREATE TABLE cleanup_batch_excluded (
                    id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
                    value VARCHAR(30) NOT NULL
                )
                """);
        DatabaseCleaner.clearTableCache();

        try {
            jdbcTemplate.update("INSERT INTO cleanup_batch_target (value) VALUES (?)", "to-clean");
            jdbcTemplate.update("INSERT INTO cleanup_batch_excluded (value) VALUES (?)", "keep");

            DatabaseCleaner.clean(cleanerDataSource.dataSource(), new String[] {"cleanup_batch_excluded"});

            assertThat(jdbcTemplate.queryForObject(
                    "SELECT COUNT(*) FROM cleanup_batch_target", Integer.class))
                    .isZero();
            assertThat(jdbcTemplate.queryForObject(
                    "SELECT value FROM cleanup_batch_excluded WHERE id = 1", String.class))
                    .isEqualTo("keep");

            jdbcTemplate.update("INSERT INTO cleanup_batch_target (value) VALUES (?)", "after-clean");
            assertThat(jdbcTemplate.queryForObject(
                    "SELECT id FROM cleanup_batch_target WHERE value = ?", Long.class, "after-clean"))
                    .isEqualTo(1L);
        } finally {
            jdbcTemplate.execute("DROP TABLE IF EXISTS cleanup_batch_excluded");
            jdbcTemplate.execute("DROP TABLE IF EXISTS cleanup_batch_target");
            DatabaseCleaner.clearTableCache();
        }
    }

    @SpringBootConfiguration
    @EnableAutoConfiguration
    static class TestApplication {
    }
}
