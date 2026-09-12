package com.empresa.platform.testing.extension;

import com.empresa.platform.testing.context.TestContext;
import io.restassured.RestAssured;
import org.junit.jupiter.api.extension.AfterEachCallback;
import org.junit.jupiter.api.extension.BeforeEachCallback;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.springframework.context.ApplicationContext;
import org.springframework.core.env.Environment;
import org.springframework.test.context.junit.jupiter.SpringExtension;

public final class PlatformIntegrationExtension implements BeforeEachCallback, AfterEachCallback {

    @Override
    public void beforeEach(ExtensionContext context) {
        TestContext.reset();
        ApplicationContext applicationContext = SpringExtension.getApplicationContext(context);
        Environment environment = applicationContext.getEnvironment();
        RestAssured.port = environment.getRequiredProperty("local.server.port", Integer.class);
    }

    @Override
    public void afterEach(ExtensionContext context) {
        TestContext.reset();
        RestAssured.reset();
    }
}
