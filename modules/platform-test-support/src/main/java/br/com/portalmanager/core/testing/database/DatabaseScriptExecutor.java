package br.com.portalmanager.core.testing.database;

import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceLoader;
import org.springframework.jdbc.datasource.init.ResourceDatabasePopulator;

import javax.sql.DataSource;
import java.util.Arrays;

public final class DatabaseScriptExecutor {

    private DatabaseScriptExecutor() {
    }

    public static void execute(
            DataSource dataSource,
            ResourceLoader resourceLoader,
            String[] scripts,
            boolean continueOnError
    ) {
        if (scripts == null || scripts.length == 0) {
            return;
        }

        Resource[] resources = Arrays.stream(scripts)
                .map(String::trim)
                .filter(location -> !location.isEmpty())
                .map(resourceLoader::getResource)
                .toArray(Resource[]::new);

        if (resources.length == 0) {
            return;
        }

        Arrays.stream(resources).forEach(DatabaseScriptExecutor::validate);

        ResourceDatabasePopulator populator = new ResourceDatabasePopulator(resources);
        populator.setContinueOnError(continueOnError);
        populator.execute(dataSource);
    }

    private static void validate(Resource resource) {
        if (!resource.exists() || !resource.isReadable()) {
            throw new IllegalArgumentException(
                    "Database script does not exist or is not readable: " + resource.getDescription()
            );
        }
    }
}
