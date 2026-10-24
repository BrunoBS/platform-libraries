package br.com.portalmanager.platform.library.testing.http;

import com.github.tomakehurst.wiremock.WireMockServer;
import com.github.tomakehurst.wiremock.client.MappingBuilder;
import com.github.tomakehurst.wiremock.matching.RequestPatternBuilder;
import com.github.tomakehurst.wiremock.stubbing.StubMapping;

import static com.github.tomakehurst.wiremock.core.WireMockConfiguration.wireMockConfig;

/** A single, context-scoped HTTP dependency simulator on a dynamic port. */
public final class HttpMockServer {

    private final WireMockServer server = new WireMockServer(wireMockConfig().dynamicPort());

    public void start() {
        server.start();
    }

    public void stop() {
        if (!server.isRunning()) {
            return;
        }
        try {
            server.stop();
        } catch (RuntimeException failure) {
            // Jetty can report STOPPED while completing graceful shutdown on JDK 25.
            if (server.isRunning()) {
                throw failure;
            }
        }
    }

    public boolean isRunning() {
        return server.isRunning();
    }

    public int port() {
        return server.port();
    }

    public String baseUrl() {
        return server.baseUrl();
    }

    public void reset() {
        server.resetAll();
    }

    public StubMapping stubFor(MappingBuilder mapping) {
        return server.stubFor(mapping);
    }

    public void verify(int count, RequestPatternBuilder request) {
        server.verify(count, request);
    }

    public void verify(RequestPatternBuilder request) {
        server.verify(request);
    }
}
