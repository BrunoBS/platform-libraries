package com.empresa.platform.testing.mysql;

import com.empresa.platform.testing.annotation.WithDatabaseScripts;
import com.empresa.platform.testing.annotation.WithMySql;
import com.empresa.platform.testing.database.CleanupMode;
import com.empresa.platform.testing.database.DatabaseCleanupPhase;
import com.empresa.platform.testing.database.DatabaseCleaner;
import com.empresa.platform.testing.database.DatabaseScriptExecutor;
import com.empresa.platform.testing.database.DatabaseSetupPhase;
import org.junit.jupiter.api.extension.AfterAllCallback;
import org.junit.jupiter.api.extension.AfterEachCallback;
import org.junit.jupiter.api.extension.BeforeAllCallback;
import org.junit.jupiter.api.extension.BeforeEachCallback;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.junit.platform.commons.support.AnnotationSupport;
import org.springframework.context.ApplicationContext;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import javax.sql.DataSource;
import java.util.Optional;

public final class MySqlTestExtension implements BeforeAllCallback, BeforeEachCallback, AfterEachCallback, AfterAllCallback {

    @Override
    public void beforeAll(ExtensionContext context) {
        scripts(context).ifPresent(configuration -> {
            if (configuration.setupPhase() == DatabaseSetupPhase.BEFORE_TEST_CLASS) {
                execute(context, configuration.setup(), configuration.continueOnError());
            }
        });
    }

    @Override
    public void beforeEach(ExtensionContext context) {
        WithMySql configuration = configuration(context);
        if (configuration.cleanup() == CleanupMode.BEFORE_EACH) {
            clean(context, configuration);
        }
        scripts(context).ifPresent(scripts -> {
            if (scripts.setupPhase() == DatabaseSetupPhase.BEFORE_EACH) {
                execute(context, scripts.setup(), scripts.continueOnError());
            }
        });
    }

    @Override
    public void afterEach(ExtensionContext context) {
        scripts(context).ifPresent(scripts -> {
            if (scripts.cleanupPhase() == DatabaseCleanupPhase.AFTER_EACH) {
                execute(context, scripts.cleanup(), scripts.continueOnError());
            }
        });
        WithMySql configuration = configuration(context);
        if (configuration.cleanup() == CleanupMode.AFTER_EACH) {
            clean(context, configuration);
        }
    }

    @Override
    public void afterAll(ExtensionContext context) {
        scripts(context).ifPresent(configuration -> {
            if (configuration.cleanupPhase() == DatabaseCleanupPhase.AFTER_TEST_CLASS) {
                execute(context, configuration.cleanup(), configuration.continueOnError());
            }
        });
    }

    private void clean(ExtensionContext context, WithMySql configuration) {
        ApplicationContext applicationContext = SpringExtension.getApplicationContext(context);
        DataSource dataSource = applicationContext.getBean(DataSource.class);
        DatabaseCleaner.clean(dataSource, configuration.excludeTables());
    }

    private void execute(ExtensionContext context, String[] locations, boolean continueOnError) {
        ApplicationContext applicationContext = SpringExtension.getApplicationContext(context);
        DatabaseScriptExecutor.execute(
                applicationContext.getBean(DataSource.class),
                applicationContext,
                locations,
                continueOnError
        );
    }

    private Optional<WithDatabaseScripts> scripts(ExtensionContext context) {
        return AnnotationSupport.findAnnotation(context.getRequiredTestClass(), WithDatabaseScripts.class);
    }

    private WithMySql configuration(ExtensionContext context) {
        return AnnotationSupport.findAnnotation(context.getRequiredTestClass(), WithMySql.class)
                .orElseThrow(() -> new IllegalStateException("@WithMySql configuration was not found"));
    }
}
