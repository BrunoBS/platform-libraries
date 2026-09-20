package br.com.portalmanager.core.testing.extension;

import br.com.portalmanager.core.testing.context.TestContext;
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
