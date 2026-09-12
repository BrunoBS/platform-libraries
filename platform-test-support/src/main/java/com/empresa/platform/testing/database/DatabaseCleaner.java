package com.empresa.platform.testing.database;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

public final class DatabaseCleaner {

    private static final String TABLES_QUERY = """
            SELECT TABLE_NAME
              FROM INFORMATION_SCHEMA.TABLES
             WHERE TABLE_SCHEMA = DATABASE()
               AND TABLE_TYPE = 'BASE TABLE'
            """;

    private DatabaseCleaner() {
    }

    public static void clean(DataSource dataSource, String[] excludedTables) {
        Set<String> excluded = new HashSet<>();
        Arrays.stream(excludedTables)
                .map(name -> name.toLowerCase(Locale.ROOT))
                .forEach(excluded::add);

        try (Connection connection = dataSource.getConnection()) {
            boolean autoCommit = connection.getAutoCommit();
            connection.setAutoCommit(true);
            try {
                List<String> tables = findTables(connection, excluded);
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
            } finally {
                connection.setAutoCommit(autoCommit);
            }
        } catch (SQLException exception) {
            throw new IllegalStateException("Could not clean the MySQL test database", exception);
        }
    }

    private static List<String> findTables(Connection connection, Set<String> excluded) throws SQLException {
        List<String> tables = new ArrayList<>();
        try (PreparedStatement statement = connection.prepareStatement(TABLES_QUERY);
             ResultSet resultSet = statement.executeQuery()) {
            while (resultSet.next()) {
                String table = resultSet.getString(1);
                if (!excluded.contains(table.toLowerCase(Locale.ROOT))) {
                    tables.add(table);
                }
            }
        }
        return tables;
    }

    private static String quote(String identifier) {
        return "`" + identifier.replace("`", "``") + "`";
    }
}
