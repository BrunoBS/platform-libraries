package br.com.portalmanager.platform.library.testing.http;

import io.restassured.builder.RequestSpecBuilder;

@FunctionalInterface
public interface PlatformRequestSpecificationCustomizer {

    void customize(RequestSpecBuilder builder);
}
