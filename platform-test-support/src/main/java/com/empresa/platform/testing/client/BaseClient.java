package com.empresa.platform.testing.client;

import io.restassured.specification.RequestSpecification;

public abstract class BaseClient {

    private final PlatformRequestSpecificationFactory requests;

    protected BaseClient(PlatformRequestSpecificationFactory factory) {
        this.requests = factory;
    }

    protected final RequestSpecification request() {
        return requests.create();
    }

    protected final RequestSpecification authorizedRequest() {
        return requests.createAuthorized();
    }

    protected final RequestSpecification authorizedRequest(AuthorizationRequestData authorization) {
        return requests.createAuthorized(authorization);
    }
}
