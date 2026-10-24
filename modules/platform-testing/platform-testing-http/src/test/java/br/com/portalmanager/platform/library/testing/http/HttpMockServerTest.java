package br.com.portalmanager.platform.library.testing.http;

import org.junit.jupiter.api.Test;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo;
import static org.assertj.core.api.Assertions.assertThat;

class HttpMockServerTest {

    @Test
    void startsOnDynamicPortExposesUrlResetsAndStops() throws Exception {
        HttpMockServer first = new HttpMockServer();
        HttpMockServer second = new HttpMockServer();
        try {
            first.start();
            second.start();
            assertThat(first.isRunning()).isTrue();
            assertThat(first.port()).isPositive().isNotEqualTo(second.port());
            assertThat(first.baseUrl()).endsWith(":" + first.port());

            first.stubFor(get(urlEqualTo("/external")).willReturn(aResponse().withStatus(201)));
            assertThat(status(first)).isEqualTo(201);
            assertThat(status(second)).isEqualTo(404);

            first.reset();
            assertThat(status(first)).isEqualTo(404);
        } finally {
            first.stop();
            second.stop();
        }
        assertThat(first.isRunning()).isFalse();
        assertThat(second.isRunning()).isFalse();
    }

    private static int status(HttpMockServer server) throws Exception {
        HttpRequest request = HttpRequest.newBuilder(URI.create(server.baseUrl() + "/external")).GET().build();
        return HttpClient.newHttpClient().send(request, HttpResponse.BodyHandlers.discarding()).statusCode();
    }
}
