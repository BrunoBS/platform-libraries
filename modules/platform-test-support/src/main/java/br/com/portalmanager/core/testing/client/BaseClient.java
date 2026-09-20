package br.com.portalmanager.core.testing.client;

import io.restassured.specification.RequestSpecification;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.json.JsonMapper;

public abstract class BaseClient {

    private static final JsonMapper JSON_MAPPER = JsonMapper.builder().build();

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

    protected final String json(Object value) {
        try {
            return JSON_MAPPER.writeValueAsString(value);
        } catch (JacksonException e) {
            throw new IllegalStateException("Unable to serialize request body with Jackson 3", e);
        }
    }
}
