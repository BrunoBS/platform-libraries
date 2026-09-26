package br.com.portalmanager.platform.library.testing.client;

import io.restassured.builder.RequestSpecBuilder;

@FunctionalInterface
public interface PlatformRequestSpecificationCustomizer {

    void customize(RequestSpecBuilder builder);
}
