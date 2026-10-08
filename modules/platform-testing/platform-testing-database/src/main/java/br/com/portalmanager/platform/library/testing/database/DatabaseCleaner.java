package br.com.portalmanager.platform.library.testing.database;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

public final class DatabaseCleaner {

    private static final Logger log = LoggerFactory.getLogger(DatabaseCleaner.class);

    private static final String TABLES_QUERY = """
            SELECT TABLE_NAME
              FROM INFORMATION_SCHEMA.TABLES
             WHERE TABLE_SCHEMA = DATABASE()
               AND TABLE_TYPE = 'BASE TABLE'
            """;

    private static final ConcurrentMap<DatabaseKey, List<String>> TABLE_CACHE =
            new ConcurrentHashMap<>();

    private DatabaseCleaner() {
    }

    public static void clean(DataSource dataSource, String[] excludedTables) {
        Instant startedAt = Instant.now();

        Set<String> excluded = new HashSet<>();
        Arrays.stream(excludedTables)
                .map(name -> name.toLowerCase(Locale.ROOT))
                .forEach(excluded::add);

        try (Connection connection = dataSource.getConnection()) {
            boolean autoCommit = connection.getAutoCommit();
            connection.setAutoCommit(true);
            try {
                DatabaseKey key = databaseKey(connection);
                List<String> allTables = TABLE_CACHE.computeIfAbsent(
                        key,
                        ignored -> findTablesUnchecked(connection)
                );

                List<String> tables = allTables.stream()
                        .filter(table -> !excluded.contains(table.toLowerCase(Locale.ROOT)))
                        .toList();

                try (Statement statement = connection.createStatement()) {
                    statement.execute("SET FOREIGN_KEY_CHECKS = 0");
                    try {
                        for (String table : tables) {
                            statement.execute("TRUNCATE TABLE " + quote(table));
                        }
                    } finally {
                        statement.execute("SET FOREIGN_KEY_CHECKS = 1");
                    }
                }

                long elapsedMs = Duration.between(startedAt, Instant.now()).toMillis();
                log.debug(
                        "[TEST-PERF] database-cleanup={}ms tables={} cache-key={}",
                        elapsedMs,
                        tables.size(),
                        key
                );
            } finally {
                connection.setAutoCommit(autoCommit);
            }
        } catch (SQLException exception) {
            throw new IllegalStateException("Could not clean the MySQL test database", exception);
        }
    }

    public static void clearTableCache() {
        TABLE_CACHE.clear();
    }

    static int cachedDatabaseCount() {
        return TABLE_CACHE.size();
    }

    private static List<String> findTablesUnchecked(Connection connection) {
        try {
            return List.copyOf(findTables(connection));
        } catch (SQLException exception) {
            throw new IllegalStateException("Could not discover MySQL test tables", exception);
        }
    }

    private static List<String> findTables(Connection connection) throws SQLException {
        List<String> tables = new ArrayList<>();
        try (PreparedStatement statement = connection.prepareStatement(TABLES_QUERY);
             ResultSet resultSet = statement.executeQuery()) {
            while (resultSet.next()) {
                tables.add(resultSet.getString(1));
            }
        }
        return tables;
    }

    private static DatabaseKey databaseKey(Connection connection) throws SQLException {
        DatabaseMetaData metadata = connection.getMetaData();
        return new DatabaseKey(
                metadata.getURL(),
                connection.getCatalog()
        );
    }

    private static String quote(String identifier) {
        return "`" + identifier.replace("`", "``") + "`";
    }

    private record DatabaseKey(String jdbcUrl, String catalog) {
    }
}
