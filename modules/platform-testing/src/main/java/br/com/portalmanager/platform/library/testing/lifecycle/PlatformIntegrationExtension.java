package br.com.portalmanager.platform.library.testing.lifecycle;

import br.com.portalmanager.platform.library.testing.context.TestContext;
import org.junit.jupiter.api.extension.AfterEachCallback;
import org.junit.jupiter.api.extension.BeforeEachCallback;
import org.junit.jupiter.api.extension.ExtensionContext;

public final class PlatformIntegrationExtension implements BeforeEachCallback, AfterEachCallback {

    @Override
    public void beforeEach(ExtensionContext context) {
        TestContext.reset();
    }

    @Override
    public void afterEach(ExtensionContext context) {
        TestContext.reset();
    }
}
