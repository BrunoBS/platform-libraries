package br.com.portalmanager.platform.library.testing.database;

import org.springframework.jdbc.datasource.DriverManagerDataSource;
import org.testcontainers.containers.MySQLContainer;

import javax.sql.DataSource;
import java.util.Objects;

final class MySqlCleanerDataSource {

    private final MySQLContainer<?> container;
    private volatile DataSource dataSource;

    MySqlCleanerDataSource(MySQLContainer<?> container) {
        this.container = Objects.requireNonNull(container);
    }

    DataSource dataSource() {
        DataSource current = dataSource;
        if (current != null) {
            return current;
        }

        synchronized (this) {
            current = dataSource;
            if (current == null) {
                String jdbcUrl = container.getJdbcUrl();
                String separator = jdbcUrl.contains("?") ? "&" : "?";
                DriverManagerDataSource cleanerDataSource = new DriverManagerDataSource();
                cleanerDataSource.setUrl(jdbcUrl + separator + "rewriteBatchedStatements=true");
                cleanerDataSource.setUsername(container.getUsername());
                cleanerDataSource.setPassword(container.getPassword());
                current = cleanerDataSource;
                dataSource = current;
            }
        }

        return current;
    }
}
