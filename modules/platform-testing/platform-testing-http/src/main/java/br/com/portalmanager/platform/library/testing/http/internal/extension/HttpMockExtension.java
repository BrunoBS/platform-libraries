package br.com.portalmanager.platform.library.testing.http.internal.extension;
import br.com.portalmanager.platform.library.testing.http.api.mock.HttpMockServer;

import org.junit.jupiter.api.extension.BeforeEachCallback;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.springframework.test.context.junit.jupiter.SpringExtension;

public final class HttpMockExtension implements BeforeEachCallback {

    @Override
    public void beforeEach(ExtensionContext context) {
        SpringExtension.getApplicationContext(context).getBean(HttpMockServer.class).reset();
    }
}
