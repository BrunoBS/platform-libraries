package br.com.portalmanager.core.testing.database;

import br.com.portalmanager.core.testing.annotation.WithDatabaseScripts;
import org.junit.jupiter.api.extension.AfterAllCallback;
import org.junit.jupiter.api.extension.AfterTestExecutionCallback;
import org.junit.jupiter.api.extension.BeforeAllCallback;
import org.junit.jupiter.api.extension.BeforeTestExecutionCallback;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.junit.platform.commons.support.AnnotationSupport;
import org.springframework.context.ApplicationContext;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import javax.sql.DataSource;
import java.lang.reflect.AnnotatedElement;
import java.util.List;

public final class DatabaseScriptExtension implements
        BeforeAllCallback,
        BeforeTestExecutionCallback,
        AfterTestExecutionCallback,
        AfterAllCallback {

    @Override
    public void beforeAll(ExtensionContext context) {
        annotations(context.getRequiredTestClass()).stream()
                .filter(script -> script.setupPhase() == DatabaseSetupPhase.BEFORE_TEST_CLASS)
                .forEach(script -> execute(context, script.setup(), script.continueOnError()));
    }

    @Override
    public void beforeTestExecution(ExtensionContext context) {
        annotations(context.getRequiredTestClass()).stream()
                .filter(script -> script.setupPhase() == DatabaseSetupPhase.BEFORE_EACH)
                .forEach(script -> execute(context, script.setup(), script.continueOnError()));

        annotations(context.getRequiredTestMethod()).forEach(
                script -> execute(context, script.setup(), script.continueOnError())
        );
    }

    @Override
    public void afterTestExecution(ExtensionContext context) {
        annotations(context.getRequiredTestMethod()).reversed().forEach(
                script -> execute(context, script.cleanup(), script.continueOnError())
        );

        annotations(context.getRequiredTestClass()).reversed().stream()
                .filter(script -> script.cleanupPhase() == DatabaseCleanupPhase.AFTER_EACH)
                .forEach(script -> execute(context, script.cleanup(), script.continueOnError()));
    }

    @Override
    public void afterAll(ExtensionContext context) {
        annotations(context.getRequiredTestClass()).reversed().stream()
                .filter(script -> script.cleanupPhase() == DatabaseCleanupPhase.AFTER_TEST_CLASS)
                .forEach(script -> execute(context, script.cleanup(), script.continueOnError()));
    }

    private void execute(ExtensionContext context, String[] locations, boolean continueOnError) {
        ApplicationContext applicationContext = SpringExtension.getApplicationContext(context);
        DataSource dataSource = applicationContext.getBeanProvider(DataSource.class)
                .getIfAvailable(() -> {
                    throw new IllegalStateException(
                            "@WithDatabaseScripts requires a DataSource in the Spring test context"
                    );
                });

        DatabaseScriptExecutor.execute(
                dataSource,
                applicationContext,
                locations,
                continueOnError
        );

        if (locations != null && locations.length > 0) {
            DatabaseCleaner.clearTableCache();
        }
    }

    private List<WithDatabaseScripts> annotations(AnnotatedElement element) {
        return AnnotationSupport.findRepeatableAnnotations(element, WithDatabaseScripts.class);
    }
}
