package br.com.portalmanager.platform.library.testing.http;

import br.com.portalmanager.platform.library.testing.http.response.BaseResponse;
import io.restassured.response.ValidatableResponse;
import io.restassured.specification.RequestSpecification;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.json.JsonMapper;

import java.util.Objects;
import java.util.function.Function;

public abstract class BaseClient {

    private final PlatformRequestSpecificationFactory requests;
    private final JsonMapper jsonMapper;

    protected BaseClient(PlatformRequestSpecificationFactory factory, JsonMapper jsonMapper) {
        this.requests = Objects.requireNonNull(factory, "Request specification factory must not be null");
        this.jsonMapper = Objects.requireNonNull(jsonMapper, "JSON mapper must not be null");
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

    protected final <R extends BaseResponse<R>> R response(
            ValidatableResponse response,
            Function<ValidatableResponse, R> responseFactory
    ) {
        Objects.requireNonNull(response, "Response must not be null");
        Objects.requireNonNull(responseFactory, "Response factory must not be null");
        return responseFactory.apply(response);
    }

    protected final String json(Object value) {
        try {
            return jsonMapper.writeValueAsString(value);
        } catch (JacksonException e) {
            throw new IllegalStateException("Unable to serialize request body with Jackson 3", e);
        }
    }
}
