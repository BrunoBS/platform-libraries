package br.com.portalmanager.core.testing.unit;

import br.com.portalmanager.core.testing.context.TestContext;
import org.junit.jupiter.api.extension.AfterEachCallback;
import org.junit.jupiter.api.extension.BeforeEachCallback;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.slf4j.MDC;

public final class PlatformUnitTestExtension implements BeforeEachCallback, AfterEachCallback {

    @Override
    public void beforeEach(ExtensionContext context) {
        clearContexts();
    }

    @Override
    public void afterEach(ExtensionContext context) {
        clearContexts();
    }

    private void clearContexts() {
        TestContext.reset();
        MDC.clear();
    }
}
