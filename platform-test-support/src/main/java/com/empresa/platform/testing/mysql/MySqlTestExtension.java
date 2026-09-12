package com.empresa.platform.testing.mysql;

import com.empresa.platform.testing.annotation.WithMySql;
import com.empresa.platform.testing.database.CleanupMode;
import com.empresa.platform.testing.database.DatabaseCleaner;
import org.junit.jupiter.api.extension.AfterEachCallback;
import org.junit.jupiter.api.extension.BeforeEachCallback;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.junit.platform.commons.support.AnnotationSupport;
import org.springframework.context.ApplicationContext;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import javax.sql.DataSource;

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
        DataSource dataSource = applicationContext.getBean(DataSource.class);
        DatabaseCleaner.clean(dataSource, configuration.excludeTables());
    }

    private WithMySql configuration(ExtensionContext context) {
        return AnnotationSupport.findAnnotation(context.getRequiredTestClass(), WithMySql.class)
                .orElseThrow(() -> new IllegalStateException("@WithMySql configuration was not found"));
    }
}
