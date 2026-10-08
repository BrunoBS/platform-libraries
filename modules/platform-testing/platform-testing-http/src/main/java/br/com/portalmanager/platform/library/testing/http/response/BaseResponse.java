package br.com.portalmanager.platform.library.testing.http.response;

import io.restassured.response.ValidatableResponse;
import org.hamcrest.Matchers;

public abstract class BaseResponse<T extends BaseResponse<T>> {

    protected final ValidatableResponse response;

    protected BaseResponse(ValidatableResponse response) {
        this.response = response;
    }

    @SuppressWarnings("unchecked")
    protected T self() {
        return (T) this;
    }

    public T expectOk() {
        response.statusCode(200);
        return self();
    }

    public T expectCreated() {
        response.statusCode(201);
        return self();
    }

    public T expectNoContent() {
        response.statusCode(204);
        return self();
    }

    public T expectBadRequest() {
        response.statusCode(400);
        return self();
    }

    public T expectUnauthorized() {
        response.statusCode(401);
        return self();
    }

    public T expectForbidden() {
        response.statusCode(403);
        return self();
    }

    public T expectNotFound() {
        response.statusCode(404);
        return self();
    }

    public T expectConflict() {
        response.statusCode(409);
        return self();
    }

    public T expect2xx() {
        response.statusCode(Matchers.allOf(Matchers.greaterThanOrEqualTo(200), Matchers.lessThan(300)));
        return self();
    }

    public T expect4xx() {
        response.statusCode(Matchers.allOf(Matchers.greaterThanOrEqualTo(400), Matchers.lessThan(500)));
        return self();
    }

    public T expect5xx() {
        response.statusCode(Matchers.allOf(Matchers.greaterThanOrEqualTo(500), Matchers.lessThan(600)));
        return self();
    }

    public T expect(String path, Object value) {
        response.body(path, Matchers.is(value));
        return self();
    }

    public T expectNotNull(String path) {
        response.body(path, Matchers.notNullValue());
        return self();
    }

    public T expectContains(String path, String value) {
        response.body(path, Matchers.containsString(value));
        return self();
    }

    public T expectSize(String path, int size) {
        response.body(path, Matchers.hasSize(size));
        return self();
    }

    public <R> R extract(Class<R> type) {
        return response.extract().as(type);
    }

    public ValidatableResponse response() {
        return response;
    }
}
