package br.com.portalmanager.platform.library.schemavalidation.web;

import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpInputMessage;

import java.io.ByteArrayInputStream;
import java.io.InputStream;

final class CachedBodyHttpInputMessage implements HttpInputMessage {

    private final HttpHeaders headers;
    private final byte[] body;

    CachedBodyHttpInputMessage(HttpHeaders headers, byte[] body) {
        this.headers = headers;
        this.body = body;
    }

    @Override
    public InputStream getBody() {
        return new ByteArrayInputStream(body);
    }

    @Override
    public HttpHeaders getHeaders() {
        return headers;
    }
}
