package br.com.portalmanager.platform.library.testing.database;

import br.com.portalmanager.platform.library.testing.database.annotation.WithMySql;

import org.junit.jupiter.api.extension.AfterEachCallback;
import org.junit.jupiter.api.extension.BeforeEachCallback;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.junit.platform.commons.support.AnnotationSupport;
import org.springframework.context.ApplicationContext;
import org.springframework.test.context.junit.jupiter.SpringExtension;

public final class MySqlTestExtension implements BeforeEachCallback, AfterEachCallback {

    @Override
    public void beforeEach(ExtensionContext context) {
        WithMySql configuration = configuration(context);
        if (configuration.cleanup() == CleanupMode.BEFORE_EACH) {
            clean(context, configuration);
        }
    }

    @Override
    public void afterEach(ExtensionContext context) {
        WithMySql configuration = configuration(context);
        if (configuration.cleanup() == CleanupMode.AFTER_EACH) {
            clean(context, configuration);
        }
    }

    private void clean(ExtensionContext context, WithMySql configuration) {
        ApplicationContext applicationContext = SpringExtension.getApplicationContext(context);
        MySqlCleanerDataSource dataSource = applicationContext.getBean(MySqlCleanerDataSource.class);
        DatabaseCleaner.clean(dataSource.dataSource(), configuration.excludeTables());
    }

    private WithMySql configuration(ExtensionContext context) {
        return AnnotationSupport.findAnnotation(context.getRequiredTestClass(), WithMySql.class)
                .orElseThrow(() -> new IllegalStateException("@WithMySql configuration was not found"));
    }
}
